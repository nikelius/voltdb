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

import geb.*
import geb.spock.*
import geb.driver.CachingDriverFactory
import org.openqa.selenium.interactions.Actions
import spock.lang.*
import groovy.json.*

/**
 * This class represents a generic VoltDB Management Center page (without
 * specifying which tab you are on), which is the main page of the web UI
 * - the new UI for version 4.9 (November 2014), replacing the old Web Studio
 * (& DB Monitor Catalog Report, etc.).
 */
class VoltDBManagementCenterPage extends Page {
    static url = 'http://localhost:8080/'
    static content = {
        navTabs { $('#nav') }
        dbMonitorTab { navTabs.find('#navDbmonitor') }
        schemaTab    { navTabs.find('#navSchema') }
        sqlQueryTab  { navTabs.find('#navSqlQuery') }
        dbMonitorLink(to: DbMonitorPage) { dbMonitorTab.find('a') }
        schemaLink   (to: SchemaPage)    { schemaTab.find('a') }
        sqlQueryLink (to: SqlQueryPage)  { sqlQueryTab.find('a') }
    }
    static at = {
        title == 'VoltDB Management Center'
        dbMonitorLink.displayed
        schemaLink.displayed
        sqlQueryLink.displayed
    }

    /**
     * Returns true if the current page is a DbMonitorPage (i.e., the "DB Monitor"
     * tab of the VoltDB Management Center page is currently open).
     * @return true if a DbMonitorPage is currently open.
     */
    def boolean isDbMonitorPageOpen() {
        if (dbMonitorTab.attr('class') == 'active') {
            return true
        } else {
            return false
        }
    }

    /**
     * Returns true if the current page is a SchemaPage (i.e., the "Schema"
     * tab of the VoltDB Management Center page is currently open).
     * @return true if a SchemaPage is currently open.
     */
    def boolean isSchemaPageOpen() {
        if (schemaTab.attr('class') == 'active') {
            return true
        } else {
            return false
        }
    }

    /**
     * Returns true if the current page is a SqlQueryPage (i.e., the "SQL Query"
     * tab of the VoltDB Management Center page is currently open).
     * @return true if a SqlQueryPage is currently open.
     */
    def boolean isSqlQueryPageOpen() {
        if (sqlQueryTab.attr('class') == 'active') {
            return true
        } else {
            return false
        }
    }

    // TODO: fix this!
    def VoltDBManagementCenterPage getPage() {
        if (isDbMonitorPageOpen()) {
            return (DbMonitorPage) this
        } else if(isSchemaPageOpen()) {
            return (SchemaPage) this
        } else if(isSqlQueryPageOpen()) {
            return (SqlQueryPage) this
        } else {
            return null;
        }
    }
    
    def void openDbMonitorPage() {
        if (!isDbMonitorPageOpen()) {
            dbMonitorLink.click()
        }
        //return (DbMonitorPage) this
    }
    
    def void openSchemaPage() {
        if (!isSchemaPageOpen()) {
            schemaLink.click()
        }
        //return (SchemaPage) this
    }

    def void openSqlQueryPage() {
        if (!isSqlQueryPageOpen()) {
            sqlQueryLink.click()
        }
        //return (SqlQueryPage) this
    }
}
