import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class pdf {

    public static String ToHtml(File file) throws IOException {
        File tempf =null;
        Writer w =null ;
        PDDocument page = null;
        FileWriter fw =null ;
        String temp ;
            tempf = File.createTempFile("temp", ".html");
            temp = tempf.getName();
            page = PDDocument.load(file);
            PDFText2HTML conv = new PDFText2HTML();
            String html = conv.getText(page);
            fw = new FileWriter(temp);
            w = new BufferedWriter(fw);
            w.write(html);

            page.close();
            fw.close();
            w.close();

        return temp;
    }

    public static String ToTxt(File PageFile) throws IOException {
        String temp ;
        File tempf =null;
        COSDocument cosDoc = null ;
        PDDocument pdDoc =null ;
        PrintWriter pw =null ;
            tempf = File.createTempFile("temp", ".txt");
            temp = tempf.getName();
            String parsedText;
            PDFParser parser = new PDFParser(new RandomAccessFile(PageFile, "r"));
            parser.parse();
            cosDoc = parser.getDocument();

            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdDoc = new PDDocument(cosDoc);
            parsedText = pdfStripper.getText(pdDoc);
            pw = new PrintWriter(temp);
            pw.print(parsedText);

            if (cosDoc != null)
                cosDoc.close();

            if (pdDoc != null)
                pdDoc.close();
            pw.close();
        return temp;
    }

    public static String ToImage(File file) throws IOException {
        String temp ;
        File tempf =null;
        PDDocument doc= null ;
        tempf= File.createTempFile("temp",".png");
        temp = tempf.getName();
        doc = PDDocument.load(file);
        PDFRenderer pdfRenderer = new PDFRenderer(doc);
        BufferedImage bim = pdfRenderer.renderImageWithDPI(
                    0, 300, ImageType.RGB);
        ImageIOUtil.writeImage(bim, temp, 300);

        System.out.println("File downloaded");

        doc.close();

        return temp;
    }


    public static String Convert(String op, String inputurl) throws IOException {
        System.out.println("opening connection");
        URL url = new URL(inputurl);//"http://scheinerman.net/judaism/pesach/haggadah.pdf"/
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        InputStream is = con.getInputStream();
        PDDocument pdd = PDDocument.load(is);
        Splitter splitter = new Splitter();
        List<PDDocument> pages = splitter.split(pdd);
        PDDocument firstPage = pages.get(0);

        File first = new File("firstPage.pdf");
        firstPage.save(first);

        String output = (op.equals("ToImage")) ? ToImage(first) :
                (op.equals("ToText")) ? ToTxt(first) :
                        (op.equals("ToHTML")) ? ToHtml(first) : "null";
        firstPage.close();
        pdd.close();
        is.close();

        return output;

    }
}