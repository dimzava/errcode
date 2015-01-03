package errcode

import com.google.common.io.Files
import org.apache.commons.vfs2.FileObject
import org.apache.commons.vfs2.FileSystemManager
import org.apache.commons.vfs2.VFS


//import org.ysb33r.groovy.dsl.vfs.VFS

class FileUtils {

    static BufferedReader fileToReader(File file) {
        String fileName = file.name
        if(isGzipFile(fileName)) {
            return gzipToReader(file)
        } else if(isTextFile(fileName)) {
            return file.newReader()
        } else {
            throw new UnsupportedOperationException("Cannot process [${fileExtension(fileName)}] files.")
        }
    }

    static BufferedReader gzipToReader(File file) {
        FileSystemManager mgr = VFS.getManager()
        FileObject gzFile = mgr.resolveFile("gz://${file.path.replace('\\', '/')}")
        InputStream inputStream = gzFile?.content?.inputStream
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream)
        return new BufferedReader(inputStreamReader)
    }

    static boolean isGzipFile(File file) {
        return isGzipFile(file.name)
    }

    static boolean isGzipFile(String fileName) {
        return fileExtension(fileName) == 'gz'
    }

    static boolean isTextFile(File file) {
        return isTextFile(file.name)
    }

    static boolean isTextFile(String fileName) {
        String extension = fileExtension(fileName)
        return extension == 'txt' || extension == 'log'
    }

    static String fileExtension(String fullFileName) {
        return Files.getFileExtension(fullFileName)
    }

}
