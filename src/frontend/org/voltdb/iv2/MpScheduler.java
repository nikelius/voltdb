/* This file is part of VoltDB.
 * Copyright (C) 2008-2012 VoltDB Inc.
 *
 * VoltDB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * VoltDB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with VoltDB.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.voltdb.iv2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.voltcore.messaging.MessagingException;
import org.voltdb.messaging.InitiateResponseMessage;
import org.voltdb.ProcedureRunner;
import org.voltdb.SystemProcedureCatalog;
import org.voltdb.VoltDB;
import org.voltdb.VoltTable;
import org.voltdb.dtxn.TransactionState;
import org.voltdb.messaging.CompleteTransactionMessage;
import org.voltdb.messaging.FragmentResponseMessage;
import org.voltdb.messaging.FragmentTaskMessage;
import org.voltdb.messaging.Iv2InitiateTaskMessage;

public class MpScheduler extends Scheduler
{
    private Map<Long, TransactionState> m_outstandingTxns =
        new HashMap<Long, TransactionState>();
    private Map<Long, DuplicateCounter> m_duplicateCounters =
        new HashMap<Long, DuplicateCounter>();

    MpScheduler(PartitionClerk clerk)
    {
        super(clerk);
    }

    // MpScheduler expects to see initiations for multipartition procedures and
    // system procedures which are "every-partition", meaning that they run as
    // single-partition procedures at every partition, and the results are
    // aggregated/deduped here at the MPI.
    public void handleIv2InitiateTaskMessage(Iv2InitiateTaskMessage message)
    {
        final String procedureName = message.getStoredProcedureName();
        final ProcedureRunner runner = m_loadedProcs.getProcByName(procedureName);

        // HACK: grab the current sitetracker until we write leader notices.
        m_clerk = VoltDB.instance().getSiteTracker();

        // Handle every-site system procedures (at the MPI)
        if (runner.isSystemProcedure()) {
            SystemProcedureCatalog.Config cfg =
                SystemProcedureCatalog.listing.get(procedureName);
            if (cfg.everySite) {
                // Send an SP initiate task to all remote sites
                final Long localId = m_mailbox.getHSId();
                final long mpTxnId = m_txnId.incrementAndGet();
                Iv2InitiateTaskMessage sp = new Iv2InitiateTaskMessage(
                        localId, // make the MPI the initiator.
                        message.getCoordinatorHSId(),
                        mpTxnId,
                        message.isReadOnly(),
                        true, // isSinglePartition
                        message.getStoredProcedureInvocation(),
                        message.getClientInterfaceHandle());
                DuplicateCounter counter = new DuplicateCounter(
                        message.getInitiatorHSId(),
                        m_clerk.getHSIdsForPartitionInitiators().size(),
                        mpTxnId);
                m_duplicateCounters.put(mpTxnId, counter);
                for (Long hsid : m_clerk.getHSIdsForPartitionInitiators()) {
                    try {
                        System.out.println("Sending ESP to: " + hsid);
                        m_mailbox.send(hsid, sp);
                    } catch (MessagingException e) {
                        VoltDB.crashLocalVoltDB("Failed to serialize initiation for " +
                                procedureName, true, e);
                    }
                }
                return;
            }
        }

        // Multi-partition initiation (at the MPI)
        final List<Long> partitionInitiators = m_clerk.getHSIdsForPartitionInitiators();
        // Figure out the local partition initiator that we can use to do BorrowTask work
        // on our behalf.
        long buddy_hsid = m_clerk.getBuddySiteForMPI(m_mailbox.getHSId());
        final MpProcedureTask task =
            new MpProcedureTask(m_mailbox, m_loadedProcs.getProcByName(procedureName),
                    m_txnId.incrementAndGet(), m_pendingTasks, message, partitionInitiators,
                    buddy_hsid);
        m_outstandingTxns.put(task.m_txn.txnId, task.m_txn);
        m_pendingTasks.offer(task);
    }

    // The MpScheduler will see InitiateResponseMessages from the Partition masters when
    // performing an every-partition system procedure.  A consequence of this deduping
    // is that the MpScheduler will also need to forward the final InitiateResponseMessage
    // for a normal multipartition procedure back to the client interface since it must
    // see all of these messages and control their transmission.
    public void handleInitiateResponseMessage(InitiateResponseMessage message)
    {
        DuplicateCounter counter = m_duplicateCounters.get(message.getTxnId());
        if (counter != null) {
            int result = counter.offer(message);
            if (result == DuplicateCounter.DONE) {
                m_duplicateCounters.remove(message.getTxnId());
                try {
                    m_mailbox.send(counter.m_destinationId, message);
                } catch (MessagingException e) {
                    VoltDB.crashLocalVoltDB("Failed to send every-site response.", true, e);
                }
            }
            else if (result == DuplicateCounter.MISMATCH) {
                VoltDB.crashLocalVoltDB("HASH MISMATCH running every-site system procedure.", true, null);
            }
            // doing duplicate suppresion: all done.
            return;
        }

        try {
            // the initiatorHSId is the ClientInterface mailbox. Yeah. I know.
            m_mailbox.send(message.getInitiatorHSId(), message);
        }
        catch (MessagingException e) {
            hostLog.error("Failed to deliver response from execution site.", e);
        }
    }

    public void handleFragmentTaskMessage(FragmentTaskMessage message,
                                          Map<Integer, List<VoltTable>> inputDeps)
    {
        throw new RuntimeException("MpScheduler should never see a FragmentTaskMessage");
    }

    // MpScheduler will receive FragmentResponses from the partition masters, and needs
    // to offer them to the corresponding TransactionState so that the TransactionTask in
    // the runloop which is awaiting these responses can do dependency tracking and eventually
    // unblock.
    public void handleFragmentResponseMessage(FragmentResponseMessage message)
    {
        TransactionState txn = m_outstandingTxns.get(message.getTxnId());
        // We could already have received the CompleteTransactionMessage from
        // the local site and the transaction is dead, despite FragmentResponses
        // in flight from remote sites.  Drop those on the floor.
        // IZZY: After implementing BorrowTasks, I'm not sure that the above sequence
        // can actually happen any longer, but leaving this and logging it for now.
        if (txn != null) {
            ((MpTransactionState)txn).offerReceivedFragmentResponse(message);
        }
        else {
            hostLog.info("MpScheduler received a FragmentResponseMessage for a null TXN ID");
        }
    }

    public void handleCompleteTransactionMessage(CompleteTransactionMessage message)
    {
        throw new RuntimeException("MpScheduler should never see a CompleteTransactionMessage");
    }
}
