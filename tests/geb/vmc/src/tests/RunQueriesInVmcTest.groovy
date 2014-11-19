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

// TODO: temp debug:
class EchoingPageChangeListener implements PageChangeListener {
    void pageWillChange(Browser browser, Page oldPage, Page newPage) {
        println "browser '$browser' changing page from '$oldPage' to '$newPage'"
    }
}

/**
 * This class contains some initial tests....
 */
class RunQueriesInVMCTest extends GebReportingSpec {
    // TODO: temp debug??
    static sleepSecs = 1
    static numRowsToInsert = 4
/*
    private def getSqlQueryPage() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage

        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage

        // Go to (& use) SQL Query tab
        page.openSqlQueryPage()
        //return (SqlQueryPage) page
    }
*/
    static def queryFrom(SqlQueryPage sqp, List<String> tables) {
        tables.each {
            String query = 'select * from ' + it + ' limit 10'
            println "\nQuery:\n  " + query
            sqp.runQuery(query)
            def qResult = sqp.getQueryResult()
            println "Result:"
            println '  ' + qResult
        }
    }

    static def deleteFrom(SqlQueryPage sqp, List<String> tables) {
        tables.each {
            String query = 'delete from ' + it
            println "\nQuery:\n  " + query
            sqp.runQuery(query)
            def qResult = sqp.getQueryResult()
            println "Result:"
            println '  ' + qResult
        }
    }

    static def insertInto(SqlQueryPage sqp, List<String> tables, int numToInsert) {
        tables.each {
            def columns = sqp.getTableColumns(it)
            println "columns (" + it + "): " + columns
            def count = 0
            for (int i = 1; i <= numToInsert; i++) {
                String query = "insert into " + it + " values ("
                for (int j = 0; j < columns.size(); j++) {
                    if (columns.get(j).contains('varchar')) {
                        query += (j > 0 ? ", " : "") + "'a" + i + "'"
                    } else {
                        query += (j > 0 ? ", " : "") + i
                    }
                }
                query += ")"
                println "\nQuery:\n  " + query
                sqp.runQuery(query)
                def qResult = sqp.getQueryResult()
                println "Result:"
                println '  ' + qResult
            }
        }
    }

    def insertIntoAllTables() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage
        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page
        assert sqp.isSqlQueryPageOpen()
        
        def tables = sqp.getTableNames()
        println "\nTables:\n  " + tables
        deleteFrom(sqp, tables)
        insertInto(sqp, tables, numRowsToInsert)
    }

    def queryAllTablesAndViews() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage
        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page
        assert sqp.isSqlQueryPageOpen()
        
        def tables = sqp.getTableNames()
        println "\nTables:\n  " + tables
        tables.each { println "\nColumns (" + it + "): " + sqp.getTableColumns(it) }
        tables.each { println "\nColumn names (" + it + "): " + sqp.getTableColumnNames(it) }
        tables.each { println "\nColumn types (" + it + "): " + sqp.getTableColumnTypes(it) }
        queryFrom(sqp, tables)
        
        def views = sqp.getViewNames()
        println "\nViews:\n  " + views
        views.each { println "\nColumns (" + it + "): " + sqp.getViewColumns(it) }
        views.each { println "\nColumn names (" + it + "): " + sqp.getViewColumnNames(it) }
        views.each { println "\nColumn types (" + it + "): " + sqp.getViewColumnTypes(it) }
        queryFrom(sqp, views)
    }

    def myFirstTest() {
        // TODO: temp debug:
        def listener = new EchoingPageChangeListener()
        browser.registerPageChangeListener(listener)
//        println "geb.env: " + geb.env

        setup: 'Open VMC page'
        to VoltDBManagementCenterPage

        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage

        // Go to (& use) SQL Query tab
        //def sqp = page.openSqlQueryPage()
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page
        //sqp.setQueryText('select * from partitioned_table')
        sqp.runQuery('select * from contestants')
        Thread.sleep(sleepSecs * 1000)
        //sqp.clearQuery()
        //sqp.runQuery('select * from replicated_table')
        sqp.runQuery('select * from votes limit 20')
        Thread.sleep(sleepSecs * 1000)
        assert sqp.isSqlQueryPageOpen()

        // Go to Schema tab
        //def scp = sqp.openSchemaPage()
        page.openSchemaPage()
        def scp = (SchemaPage) page
        Thread.sleep(sleepSecs * 1000)
        assert scp.isSchemaPageOpen()

        // Go to DB Monitor tab
        //def dbm = scp.openDbMonitorPage()
        page.openDbMonitorPage()
        def dbm = (DbMonitorPage) page
        Thread.sleep(sleepSecs * 1000)
        assert dbm.isDbMonitorPageOpen()
    }

    def printTableViewAndStoredProcInfo() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage
        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page

        def tables = sqp.getTableNames()
        println "\nTable Names:\n" + tables
        def views = sqp.getViewNames()
        println "\nView Names:\n" + views
        def storedProcs = sqp.getStoredProcedureNames()
        println "\nStoredProcedure Names:\n" + storedProcs
    }

    def printQueryResults() {

        setup: 'Open VMC page'
        to VoltDBManagementCenterPage

        expect: 'to be on VMC page'
        at VoltDBManagementCenterPage

        // Go to (& use) SQL Query tab
        //def sqp = page.openSqlQueryPage()
        page.openSqlQueryPage()
        def sqp = (SqlQueryPage) page
        sqp.runQuery("upsert into contestants values (1, 'John Smith')")
        Thread.sleep(sleepSecs * 1000)
        sqp.runQuery("upsert into contestants values (2, 'Mary Jones')")
        Thread.sleep(sleepSecs * 1000)
        sqp.runQuery('select * from contestants')
        Thread.sleep(sleepSecs * 1000)
        def qRes = sqp.getQueryResult(0)
        println "\nQuery Result:\n" + qRes
        sqp.runQuery('select count(*) from votes;\nselect * from contestants')
        Thread.sleep(sleepSecs * 1000)
        def qResults = sqp.getQueryResults()
        println "\nQuery Results (" + qResults.size() + "):"
        qResults.each { println it }
        def lastRes = sqp.getQueryResult()
        println "\nLast Query Result:\n" + lastRes
    }
//*
    def printSomeValues() {
        setup: 'Open VMC page'
        to VoltDBManagementCenterPage
        println "\npage: " + page
        println "page.getProperties(): " + page.getProperties()
        println "page.content.getProperties(): " + page.content.getProperties()
        
        page.openSqlQueryPage()
        println "page: " + page
        println "page.getProperties(): " + page.getProperties()
        println "page.content.getProperties(): " + page.content.getProperties()
        
        page.openSchemaPage()
        println "page: " + page
        println "page.getProperties(): " + page.getProperties()
        println "page.content.getProperties(): " + page.content.getProperties()
        
        page.openDbMonitorPage()
        println "page: " + page
        println "page.getProperties(): " + page.getProperties()
        println "page.content.getProperties(): " + page.content.getProperties()
    }
//*/
}
