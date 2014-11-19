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

import geb.navigator.Navigator
import geb.waiting.WaitTimeoutException

/**
 * This class represents the 'SQL Query' tab of the VoltDB Management Center
 * page, which is the VoltDB web UI (replacing the old Web Studio).
 */
class SqlQueryPage extends VoltDBManagementCenterPage {
    static content = {
        tabArea     { $('#tabMain') }
        tabControls { tabArea.find('.tabs') }
        tablesTab   { tabControls.find("a[href='#tab1']") }
        viewsTab    { tabControls.find("a[href='#tab2']") }
        storedProcsTab  { tabControls.find("a[href='#tab3']") }
        tablesNames { tabArea.find('#accordionTable').find('h3') }
        viewsNames  { tabArea.find('#accordionViews').find('h3') }
        storedProcNames { tabArea.find('#accordionProcedures').find('h3') }
        queryInput  { $('#theQueryText') }
        runButton   { $('#runBTn') }
        clearButton { $('#clearQuery') }
        queryResHtml { $('#resultHtml') }
        queryTables (required: false) { queryResHtml.find('table') }
        queryDurHtml { $('#queryResults') }
        errorPopup  (required: false) { $('.popup') }
        errorMessage(required: false) { errorPopup.find('#errorMessage') }
    }
    static at = {
        sqlQueryTab.attr('class') == 'active'
        queryResHtml.displayed
    }

    /**
     * Returns true if the current page is indeed a SqlQueryPage (i.e., the
     * "SQL Query" tab of the VoltDB Management Center page is currently open).
     * @return true if a SqlQueryPage is currently open.
     */
    def isOpen() {
        return isSqlQueryPageOpen()
    }

    /**
     * Displays the list of Tables (by clicking the "Tables" tab).
     */
    def showTables() {
        if (!tablesNames.displayed) {
            tablesTab.click()
            waitFor { tablesNames.displayed }
        }
    }

    /**
     * Displays the list of Views (by clicking the "Views" tab).
     */
    def showViews() {
        if (!viewsNames.displayed) {
            viewsTab.click()
            waitFor { viewsNames.displayed }
        }
    }

    /**
     * Displays the list of Stored Procedures (by clicking the "Stored Procedures" tab).
     */
    def showStoredProcedures() {
        if (!storedProcNames.displayed) {
            storedProcsTab.click()
            waitFor { storedProcNames.displayed }
        }
    }

    /**
     * Returns the list of Tables (as displayed on the "Tables" tab).<p>
     * Note: as a side effect, the "Tables" tab is opened.
     * @return the list of Table names.
     */
    def List<String> getTableNames() {
        showTables()
        def names = []
        tablesNames.each { names.add(it.text()) }
        return names
    }

    /**
     * Returns the list of Views (as displayed on the "Views" tab).<p>
     * Note: as a side effect, the "Views" tab is opened.
     * @return the list of View names.
     */
    def List<String> getViewNames() {
        showViews()
        def names = []
        viewsNames.each { names.add(it.text()) }
        return names
    }

    /**
     * Returns the list of Stored Procedures (as displayed on the
     * "Stored Procedures" tab).<p>
     * Note: as a side effect, the "Stored Procedures" tab is opened.
     * @return the list of Stored Procedure names.
     */
    def List<String> getStoredProcedureNames() {
        // TODO: fix this, to handle all the hidden names
        // (getTableNames & getViewNames may also need work, when the lists are long)
        showStoredProcedures()
        def names = []
        storedProcNames.each { names.add(it.text()) }
        return names
    }

    /**
     * Returns the list of Columns (as displayed on the "Tables" or "Views"
     * tab), for the specified table or view; each column returned includes
     * both the column name and type, e.g. "CONTESTANT_NUMBER (integer)".<p>
     * Note: as a side effect, the "Tables" or "Views" tab is opened (if
     * needed), and the specified table or view is opened (if needed), and
     * then closed.
     * @param tableOrViewName - the name of the table or view whose columns
     * are to be returned.
     * @param getViewColumns - if true, get columns for the specified view,
     * rather than table.
     * @return the list of (table or view) Column names and data types.
     */
    private def List<String> getTableOrViewColumns(String tableOrViewName, boolean getViewColumns) {
        def names = null
        if (getViewColumns) {
            showViews()
            names = viewsNames
        } else {
            showTables()
            names = tablesNames
        }
        def columns = []
        names.each {
            if (it.text() == tableOrViewName) {
                def columnList = it.next()
                if (!columnList.displayed) {
                    it.click()
                    waitFor { columnList.displayed }
                }
                //Thread.sleep(2000)
                columnList.find('ul').find('li').each { columns.add(it.text()) }
                it.click()
                waitFor { !columnList.displayed }
            }
        }
        return columns
    }

    /**
     * Returns the list of Columns (as displayed on the "Tables" tab), for the
     * specified table; each column returned includes both the column name and
     * type, e.g. "CONTESTANT_NUMBER (integer)".<p>
     * Note: as a side effect, the "Tables" tab is opened (if needed), and the
     * specified table is opened (if needed), and then closed.
     * @param tableName - the name of the table whose columns are to be returned.
     * @return the list of table Column names and data types.
     */
    def List<String> getTableColumns(String tableName) {
        return getTableOrViewColumns(tableName, false)
    }

    /**
     * Returns the list of Columns (as displayed on the "Views" tab), for the
     * specified view; each column returned includes both the column name and
     * type, e.g. "CONTESTANT_NUMBER (integer)".<p>
     * Note: as a side effect, the "Views" tab is opened (if needed), and the
     * specified view is opened (if needed), and then closed.
     * @param viewName - the name of the view whose columns are to be returned.
     * @return the list of view Column names and data types.
     */
    def List<String> getViewColumns(String viewName) {
        return getTableOrViewColumns(viewName, true)
    }

    /**
     * Returns the list of Column names (as displayed on the "Tables" tab), for
     * the specified table.<p>
     * Note: as a side effect, the "Tables" tab is opened (if needed), and the
     * specified table is opened (if needed), and then closed.
     * @param tableName - the name of the table whose column names are to be
     * returned.
     * @return the list of table Column names.
     */
    def List<String> getTableColumnNames(String tableName) {
        def columns = getTableColumns(tableName)
        columns = columns.collect { it.substring(0, it.indexOf('(')).trim() }
        return columns
    }

    /**
     * Returns the list of Column data types (as displayed on the "Tables"
     * tab), for the specified table.<p>
     * Note: as a side effect, the "Tables" tab is opened (if needed), and the
     * specified table is opened (if needed), and then closed.
     * @param tableName - the name of the table whose column types are to be
     * returned.
     * @return the list of table Column data types.
     */
    def List<String> getTableColumnTypes(String tableName) {
        def columns = getTableColumns(tableName)
        columns = columns.collect { it.substring(it.indexOf('(')+1).replace(")", "").trim() }
        return columns
    }

    /**
     * Returns the list of Column names (as displayed on the "Views" tab), for
     * the specified view.<p>
     * Note: as a side effect, the "Views" tab is opened (if needed), and the
     * specified view is opened (if needed), and then closed.
     * @param viewName - the name of the view whose column names are to be
     * returned.
     * @return the list of view Column names.
     */
    def List<String> getViewColumnNames(String viewName) {
        def columns = getViewColumns(viewName)
        columns = columns.collect { it.substring(0, it.indexOf('(')).trim() }
        return columns
    }

    /**
     * Returns the list of Column data types (as displayed on the "Views"
     * tab), for the specified view.<p>
     * Note: as a side effect, the "Views" tab is opened (if needed), and the
     * specified view is opened (if needed), and then closed.
     * @param viewName - the name of the view whose column types are to be
     * returned.
     * @return the list of view Column data types.
     */
    def List<String> getViewColumnTypes(String viewName) {
        def columns = getViewColumns(viewName)
        columns = columns.collect { it.substring(it.indexOf('(')+1).replace(")", "").trim() }
        return columns
    }

    /**
     * Enters the specified query text into the Query textarea.
     * @param queryText
     */
    def setQueryText(def queryText) {
        queryInput.value(queryText)
    }

    /**
     * Returns the current contents of the Query textarea.
     * @return the current contents of the Query textarea.
     */
    def String getQueryText() {
        return queryInput.value()
    }

    /**
     * Clears the Query text (by clicking the "Clear" button).
     */
    def clearQuery() {
        clearButton.click()
    }

    /**
     * Runs whatever query is currently listed in the Query text
     * (by clicking the "Run" button).
     */
    def runQuery() {
        String initQueryResultText = queryResHtml.text()
        String initQueryDurationText = queryDurHtml.text()
        runButton.click()
        try {
            waitFor() {
                queryResHtml.text() != null && queryDurHtml.text() != null && (queryResHtml.text() != initQueryResultText || queryDurHtml.text() != initQueryDurationText)
            }
        } catch (WaitTimeoutException e) {
            println "\nIn SqlQueryPage.runQuery(), caught WaitTimeoutException; see standard error for stack trace."
            e.printStackTrace()
            println "This is probably nothing to worry about.\n"
        }
        return this
    }

    def runQuery(String queryText) {
        //clearQuery()
        setQueryText(queryText)
        runQuery()
    }
    
    private def Map<String,List<String>> getTableResult(Navigator tableElement) {
        //println "tableElement: " + tableElement
        //println "tableElement.getClass: " + tableElement.getClass()
        def result = [:]
        def columnHeaders = tableElement.find('thead').find('th')*.text()
        //println "columnHeaders: " + columnHeaders
        columnHeaders = columnHeaders.collect {it.toLowerCase()}
        //println "columnHeaders: " + columnHeaders
        def rows = tableElement.find('tbody').find('tr')
        //println "rows: " + rows
        def colNum = 0
        def makeColumn = { index,rowset -> rowset.collect { row -> row.find('td',index).text() } }
        columnHeaders.each { result.put(it, makeColumn(colNum++, rows)) }
        //println "result: " + result
        return result
    }

    def List<Map<String,List<String>>> getQueryResults() {
        def results = []
        queryTables.each { results.add(getTableResult(it)) }
        return results
    }

    def Map<String,List<String>> getQueryResult(int index) {
        return getQueryResults().get(index)
    }

    def Map<String,List<String>> getQueryResult() {
        //println "queryResHtml: " + queryResHtml
        //println "queryTables: " + queryTables
        //println "queryResHtml.text: " + queryResHtml.text()
        //println "queryTables.size: " + queryTables.size()
        //def qTables = queryResHtml.find('table')
        //println "qTables: " + qTables
        //println "qTables.size: " + qTables.size()
        //println "qTables.first: " + qTables.first()
        return getTableResult(queryTables.last())
    }
}
