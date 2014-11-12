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

package vmcTest.tests

import geb.*
import geb.spock.*
import geb.driver.CachingDriverFactory
import org.openqa.selenium.interactions.Actions
import spock.lang.*
import groovy.json.*
import vmcTest.pages.*

/*
 * This class contains some initial tests....
 */
class RunQueriesInVMC extends GebReportingSpec {
    
    def myFirstTest() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage

        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage

        // Go to (& use) SQL Query tab
        //def sqp = page.openSqlQueryPage()
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page
        sqp.setQueryText('Foo Bar')
        Thread.sleep(5 * 1000)
        //sqp.clearQuery()
        sqp.runQuery('select * from replicated_table')
        Thread.sleep(5 * 1000)
        assert sqp.isSqlQueryPageOpen()

        // Go to Schema tab
        //def scp = sqp.openSchemaPage()
        page.openSchemaPage()
        def scp = (SchemaPage) page
        Thread.sleep(5 * 1000)
        assert scp.isSchemaPageOpen()

        // Go to DB Monitor tab
        //def dbm = scp.openDbMonitorPage()
        page.openDbMonitorPage()
        def dbm = (DbMonitorPage) page
        Thread.sleep(5 * 1000)
        assert dbm.isDbMonitorPageOpen()
    }
}
