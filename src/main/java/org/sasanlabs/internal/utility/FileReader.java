package org.sasanlabs.internal.utility;

import org.sasanlabs.service.vulnerability.fileupload.UnrestrictedFileUpload;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;


public class FileReader {

    private UnrestrictedFileUpload unrestrictedFileUpload;

    public FileReader(UnrestrictedFileUpload unrestrictedFileUpload) {
        this.unrestrictedFileUpload = unrestrictedFileUpload;
    }


    public InputStream readAsStream(String fileName) throws FileNotFoundException {
        return (
                new FileInputStream(
                        unrestrictedFileUpload.getContentDispositionRoot().toFile()
                                + FrameworkConstants.SLASH
                                + fileName));
    }

}

