package org.cdlflex.jena.excel;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelHelper {

    /**
     * Read a xlsx file and return an XSSFWorkbook format for further use.
     * 
     * @param fileString
     * @return
     */
    public static XSSFWorkbook readFile(String fileString) {
        try {
            InputStream inp = new FileInputStream(fileString);
            return new XSSFWorkbook(inp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write out an Xlsx file
     * 
     * @param wb
     * @param fileString
     * @return
     */
    public static void writeFile(Workbook wb, String fileString) {
        try {
            FileOutputStream fileOut = new FileOutputStream(fileString);
            wb.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
