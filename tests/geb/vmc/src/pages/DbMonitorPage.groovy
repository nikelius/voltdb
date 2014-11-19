/* This file is part of VoltDB.
 * Copyright (C) 2008-2014 VoltDB Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package vmcTest.pages

/**
 * This class represents the 'DB Monitor' tab of the VoltDB Management Center
 * page, which is the VoltDB web UI (replacing the old Management Center).
 */
class DbMonitorPage extends VoltDBManagementCenterPage {
    //static content = {
        //dbMonitorTab { $('#navDbmonitor') }
        //dbMonitorLink   { dbMonitorTab.find('a') }
    //}
    static at = {
        dbMonitorTab.attr('class') == 'active'
        //queryResult.displayed
    }

    /**
     * Returns true if the current page is indeed a DbMonitorPage (i.e., the
     * "DB Monitor" tab of the VoltDB Management Center page is currently open).
     * @return true if a DbMonitorPage is currently open.
     */
    def isOpen() {
        return isDbMonitorPageOpen()
    }
}
