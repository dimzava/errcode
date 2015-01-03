package errcode

import groovy.sql.GroovyRowResult
import groovy.sql.Sql

import javax.inject.Inject

class ErrorObjectService {

    private final Sql sql

    @Inject
    ErrorObjectService(Sql sql) {
        this.sql = sql
    }

    void createTablesMs() {
        try {
            sql.executeInsert("""
                if not exists (select null from information_schema.tables where table_name = 'error_objects')
                begin
                    create table error_objects (
                        [id] [int] IDENTITY(1,1) NOT NULL,
                        [errcode] [varchar](20) NULL,
                        [created_on] [datetime] NULL,
                        [file_host] [varchar](100) NULL,
                        [file_path] [varchar](300) NULL,
                        [file_name] [varchar](100) NULL,
                        [file_line_number] [int] NULL,
                        [headline] [varchar](300) NULL,
                        [stack_trace] [varchar](max) NULL,
                    )
                end
            """)
        } catch (e) {
            println "==================================="
            println "ERROR WHILE TRYING TO CREATE TABLES"
            println e
        }
    }

    void createTablesMySql() {
        try {
            sql.executeInsert("""
                CREATE TABLE IF NOT EXISTS `error_objects` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `errcode` varchar(50) NOT NULL,
                  `file_host` varchar(255) DEFAULT NULL,
                  `file_path` varchar(255) DEFAULT NULL,
                  `file_name` varchar(255) DEFAULT NULL,
                  `file_line_number` int(11) DEFAULT NULL,
                  `headline` varchar(255) DEFAULT NULL,
                  `stack_trace` text,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `errcode_UNIQUE` (`errcode`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
           """)
        } catch (e) {
            print e
        }
    }

    void createTables() {
        try {
            sql.executeInsert('''
                create table if not exists error_objects (
                    id int primary key auto_increment,
                    errcode varchar(50),
                    file_host varchar(255),
                    file_path varchar(255),
                    file_name varchar(255),
                    file_line_number int,
                    headline varchar(255),
                    stack_trace text
                )
            ''')
        } catch (e) {
            println "==================================="
            println "ERROR WHILE TRYING TO CREATE TABLES"
            println e
        }
    }

    void save(ErrorObject errorObject) {
        sql.executeInsert("""
            insert into error_objects
                (errcode, file_host, file_path, file_name, file_line_number, headline, stack_trace)
            values (
                $errorObject.errcode,
                $errorObject.fileHost,
                $errorObject.filePath,
                $errorObject.fileName,
                $errorObject.fileLineNumber,
                $errorObject.headline,
                $errorObject.stackTrace
            )
        """)
    }

    ErrorObject findByErrcode(String errcode) {
        if(!errcode || errcode.trim() == '') {
            return null
        }
        def row = sql.firstRow("select * from error_objects where errcode = $errcode")
        if(!row) {
            return null
        }
        return new ErrorObject(
            errcode: row?.errcode,
            fileHost: row?.file_host,
            filePath: row?.file_path,
            fileName: row?.file_name,
            fileLineNumber: row?.file_line_number,
            headline: row?.headline,
            stackTrace: row?.stack_trace
        )
    }

    List<ErrorObject> list() {
        List<ErrorObject> errorObjects = sql.rows("select * from error_objects").collect { GroovyRowResult row ->
                try {
                    return new ErrorObject(
                            errcode: row?.errcode,
                            fileHost: row?.file_host,
                            filePath: row?.file_path,
                            fileName: row?.file_name,
                            fileLineNumber: row?.file_line_number,
                            headline: row?.headline,
//                            stackTrace: row?.stack_trace?.characterStream?.text
                            stackTrace: row?.stack_trace
                    )
                } catch (e) {
                    println e
                }
        }
        return errorObjects
    }

    int count() {
        def row = sql.firstRow("select count(*) as errorObjectsCount from error_objects")
        return row.errorObjectsCount
    }

}
