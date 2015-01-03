package errcode

import com.fasterxml.jackson.annotation.JsonIgnore
import org.ysb33r.groovy.dsl.vfs.VFS

import java.util.regex.Pattern

class FileTransfer {

    @JsonIgnore URI srcUri
    @JsonIgnore URI destUri
    String[] fileExtensions
    List<String> errors = []
    boolean isTransferred

    FileTransfer(String srcUri, String destUri) {
        this.srcUri = parseUri(srcUri)
        this.destUri = parseUri(destUri)
        this.download()
    }

    FileTransfer(String srcUri, String destUri, String[] fileExtensions) {
        this.srcUri = parseUri(srcUri)
        this.destUri = parseUri(destUri)
        this.fileExtensions = fileExtensions
        this.download()
    }

    String getSrcUriMasked() {
        return mask(srcUri)
    }

    String getDestUriMasked() {
        return mask(destUri)
    }

    @JsonIgnore
    String getSrcUriHost() {
        return srcUri.host
    }

    @JsonIgnore
    String getSrcUriPath() {
        return srcUri.path
    }

    Integer getFileCount() {
        return new File(destUri.toString())?.list()?.length
    }

    private URI parseUri(String uriStr) {
        URI uri
        try {
            uri = new URI(uriStr)
        } catch (e) {
            errors.add(e.message)
        }
        return uri
    }

    private void download() {
        String scheme = srcUri.scheme
        try {
            // downloaded files are split in dirs locally, based on the original src uri's host+path
            destUri = new URI(normalizeUri(destUri, "/${srcUri.host}${srcUri.path}"))
            "${scheme.toLowerCase()}Download"()
            isTransferred = true
        } catch (MissingMethodException mme) {
            errors.add("Scheme [$scheme] not suppported.".toString())
        } catch(e) {
            errors.add(e.message)
        }
    }

    /**
     * TODO
     * Fail fast when srcUri is dir and no file extensions have been provided.
     * Log warning when srcUri is file and file extensions have been provided
     */
    private void scpDownload() {
        File toDir = new File(normalizeUri(destUri))
        if(!toDir.exists()) {
            toDir.mkdirs()
        }
        AntBuilder ant = new AntBuilder()
        ant.scp(
            file: "${normalizeScpUri(srcUri)}${scpFileExtensionsRegex()}",
            todir: toDir.toString(),
            trust: true
        )
    }

    /**
     * TODO
     * Log warning when srcUri is file and file extensions have been provided.
     * Groovy VFS seems to have a bug with smash=true on Mac OS (files are copied one level above the dest dir)
     */
    private void smbDownload() {
        VFS vfs = new VFS()
        vfs {
            extend {
                provider className: 'org.ysb33r.groovy.vfsplugin.smb.SmbFileProvider', schemes: ['smb','cifs']
            }
            def opts = [smash: true, overwrite: true, recursive: true, filter: smbFileExtensionsRegex()]
            cp(opts, normalizeSmbUri(srcUri), new File(normalizeUri(destUri)))
        }
    }

    private String normalizeScpUri(URI uri) {
        String uriNormalized = normalizeUri(uri)
        return uriNormalized?.replace("${uri.scheme}://", '')
    }

    private String normalizeSmbUri(URI uri) {
        String uriNormalized = normalizeUri(uri)
        return uriNormalized?.replace(';', URLEncoder.encode('\\', 'UTF-8'))
    }

    private String normalizeUri(URI uri, String suffix=null) {
        String uriStr = uri.toASCIIString()
        return suffix ? "${uriStr}${suffix}" : uriStr
    }

    /**
     * Turns the provided file extensions into a suitably formatted regex for scp.
     * E.g. something like /*.{log,gz}
     */
    private String scpFileExtensionsRegex() {
        if(!fileExtensions || fileExtensions.length == 0) {
            return ''
        }
        if(fileExtensions.length == 1) {
            return "/*.${fileExtensions[0]}"
        }
        return "/*.{${fileExtensions.join(',')}}"
    }

    /**
     * Turns the provided file extensions into a suitably formatted regex for smb.
     * E.g. something like .*\.(log|gz)$
     */
    private String smbFileExtensionsRegex() {
        if(!fileExtensions || fileExtensions.length == 0) {
            return null
        }
        if(fileExtensions.length == 1) {
            return ".*\\.${fileExtensions[0]}\$"
        }
        return ".*\\.(${fileExtensions.join('|')})\$"
    }

    Pattern localFileExtensionsRegex() {
        if(!fileExtensions || fileExtensions.length == 0) {
            return null
        }
        if(fileExtensions.length == 1) {
            return ~/.*\.${fileExtensions[0]}/
        }
        return ~/.*\.(${fileExtensions.join('|')})/
    }

    /**
     * TODO
     * Check whether vfs.friendlyUri() can be used instead
     */
    private String mask(URI uri) {
        String password = extractPassword(uri)
        if(!password) {
            return uri.toASCIIString()
        }
        return uri?.toASCIIString()?.replace(password, '***')
    }

    private String extractPassword(URI uri) {
        String userInfo = uri?.userInfo
        if(!userInfo) {
            return null
        }
        int indexOfColon = userInfo.indexOf(':')
        return userInfo.substring(indexOfColon + 1)
    }

}
