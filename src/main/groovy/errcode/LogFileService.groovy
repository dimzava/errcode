package errcode

import ratpack.launch.LaunchConfig

import javax.inject.Inject
import java.text.SimpleDateFormat

class LogFileService {

    private final LaunchConfig launchConfig
    private final ErrorObjectService errorDataService
    // The URIs of the log files to parse will be retrieved from an environment variable of this name
    // whose value is a space-separated list of log file URIs (in the form of username:password@host:/path/to/file.log
    // for SCP or \\host\d$\path\to\file.log for Windows shared folders)
    static final String LOG_FILE_URIS_ENV_VAR_KEY = 'ERRCODE_APP_LOG_FILE_URIS'
    static final List<String> LOG_FILE_URIS = System.getenv(LOG_FILE_URIS_ENV_VAR_KEY)?.tokenize(' ')
    // Path of the local *root* directory where the retrieved log files should be stored
    static final String LOCAL_LOG_FILES_DIR_ENV_VAR_KEY = 'ERRCODE_APP_LOCAL_LOG_FILES_DIR'
    static final String LOCAL_LOG_FILES_DIR = System.getenv(LOCAL_LOG_FILES_DIR_ENV_VAR_KEY)
    // A new sub-directory will be created daily under the root logs dir, using this date format
    static final String LOGS_DIR_DATE_FORMAT_STR = 'yyyy-MM-dd'
    static final SimpleDateFormat LOGS_DIR_DATE_FORMAT = new SimpleDateFormat(LOGS_DIR_DATE_FORMAT_STR)
    // Processing of ErrCodes in log files starts when any of these words is found
    static final List<String> ERRCODE_START_WORDS = ['ErrCode']
    // Processing of ErrCodes in log files stops when any of these words is found
    static final List<String> ERRCODE_STOP_WORDS = ['ERROR', 'WARN', 'INFO']

    @Inject
    LogFileService(LaunchConfig launchConfig, ErrorObjectService errorDataService) {
        this.launchConfig = launchConfig
        this.errorDataService = errorDataService
    }

    List<LogFileParsingResult> all() {
        List<LogFileParsingResult> logFileParsingResults
        try {
            List<FileTransfer> logFileTransfers = downloadAllLogFiles()
            logFileParsingResults = parseLogFiles(logFileTransfers)
        } catch (e) {
            println e
        }
        return logFileParsingResults
    }

    List<LogFileParsingResult> latest() {
        List<LogFileParsingResult> logFileParsingResults
        try {
            List<FileTransfer> logFileTransfers = downloadLatestLogFiles()
            logFileParsingResults = parseLogFiles(logFileTransfers)
        } catch (e) {
            println e
        }
        return logFileParsingResults
    }

    List<LogFileParsingResult> yesterdays() {
        List<LogFileParsingResult> logFileParsingResults
        try {
            List<FileTransfer> logFileTransfers = downloadYesterdaysLogFiles()
            logFileParsingResults = parseLogFiles(logFileTransfers)
        } catch (e) {
            println e
        }
        return logFileParsingResults
    }

    private List<FileTransfer> downloadAllLogFiles() {
        return downloadLogFiles(LOG_FILE_URIS, LOCAL_LOG_FILES_DIR, ['log', 'gz'] as String[])
    }

    private List<FileTransfer> downloadLatestLogFiles() {
        return downloadLogFiles(LOG_FILE_URIS, LOCAL_LOG_FILES_DIR, ['log'] as String[])
    }

    private List<FileTransfer> downloadYesterdaysLogFiles() {
        Date today = new Date()
        Date yesterday = today.previous()
        String yesterdayFormatted = LOGS_DIR_DATE_FORMAT.format(yesterday)
        return downloadLogFiles(LOG_FILE_URIS, LOCAL_LOG_FILES_DIR, ["${yesterdayFormatted}.log.gz"] as String[])
    }

    List<FileTransfer> downloadLogFiles(List<String> srcUris, String destRootUri, String[] fileExtensions) {
        List<FileTransfer> logFileTransfers = []
        srcUris.each { String srcUri ->
            logFileTransfers.add(new FileTransfer(srcUri, destRootUri, fileExtensions))
        }
        return logFileTransfers
    }

    private List<LogFileParsingResult> parseLogFiles(List<FileTransfer> logFileTransfers) {
        List<LogFileParsingResult> parsingResults = []
        logFileTransfers.each { FileTransfer logFileTransfer ->
            if(logFileTransfer.isTransferred) {
                try {
                    File logFile = new File(logFileTransfer.destUri.toString())
                    List<LogFileParsing> logFileParsings = []
                    if(logFile.isDirectory()) {
                        logFile.eachFileMatch(logFileTransfer.localFileExtensionsRegex()) { File childLogFile ->
                            logFileParsings.add(saveErrors(logFileTransfer, childLogFile.name, FileUtils.fileToReader(childLogFile)))
                        }
                    } else if(logFile.isFile()) {
                        logFileParsings.add(saveErrors(logFileTransfer, logFile.name, FileUtils.fileToReader(logFile)))
                    }
                    parsingResults.add(new LogFileParsingResult(logFileTransfer: logFileTransfer, logFileParsings: logFileParsings))
                } catch (e) {
                    println e
                }
            } else {
                parsingResults.add(new LogFileParsingResult(logFileTransfer: logFileTransfer))
            }
        }
        return parsingResults
    }

    LogFileParsing saveErrors(FileTransfer logFileTransfer, String logFileName, BufferedReader logFileReader) {
        ErrorObject errorObject = null
        StringBuilder sb = null
        int errorsFound = 0
        int errorsSaved = 0
        boolean isNewError
        boolean stackTraceFinished
        logFileReader.eachLine { String line, int lineNumber ->
            for(startWord in ERRCODE_START_WORDS) {
                if(line.contains(startWord)) {
                    if(errorObject) {
                        errorObject.stackTrace = sb.toString()
                        try {
                            errorDataService.save(errorObject)
                            errorsSaved++
                        } catch (e) {
//                            println e
                        }
                        errorsFound++
                    }
                    errorObject = new ErrorObject(
                            fileHost: logFileTransfer.srcUriHost,
                            filePath: logFileTransfer.srcUriPath,
                            fileName: logFileName,
                            fileLineNumber: lineNumber,
                            headline: line
                    )
                    stackTraceFinished = false
                    sb = new StringBuilder(10000)
                    isNewError = true
                    break
                }
            }
            if(isNewError) {
                isNewError = false
                return // continue reading the next line
            }
            if(errorObject) {
                for(stopWord in ERRCODE_STOP_WORDS) {
                    if(line.contains(stopWord)) {
                        stackTraceFinished = true
                        break
                    }
                }
                if(!stackTraceFinished) {
                    sb.append("$line\n")
                }
            }
        }
        return new LogFileParsing(
            logFileName: logFileName,
            errorsFound: errorsFound,
            errorsSaved: errorsSaved
        )
    }

}