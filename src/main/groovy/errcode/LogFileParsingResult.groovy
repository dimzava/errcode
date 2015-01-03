package errcode

class LogFileParsingResult {

    FileTransfer logFileTransfer
    List<LogFileParsing> logFileParsings

    Integer getErrorsFoundSum() {
        if(!logFileParsings) {
            return 0
        }
        return logFileParsings.errorsFound.sum()
    }

    Integer getErrorsSavedSum() {
        if(!logFileParsings) {
            return 0
        }
        return logFileParsings.errorsSaved.sum()
    }

}
