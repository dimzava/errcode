package errcode

class ErrorObject {

    String errcode
    String fileHost
    String filePath
    String fileName
    int fileLineNumber
    String headline
    String stackTrace

    String getErrcode() {
        int lastIndexOfColon = headline?.lastIndexOf(':')
        return headline?.substring(lastIndexOfColon + 1)
    }

    @Override
    public String toString() {
        return "ErrorObject{" +
                "errcode='" + getErrcode() + '\'' +
                ", fileHost='" + fileHost + '\'' +
                ", filePath='" + filePath + '\'' +
                ", fileName='" + fileName + '\'' +
                ", fileLineNumber=" + fileLineNumber +
                ", headline='" + headline + '\'' +
                '}';
    }

}
