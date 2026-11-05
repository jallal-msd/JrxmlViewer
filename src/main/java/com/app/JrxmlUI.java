
package com.app;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.export.SimpleDocxExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

class JrxmlUI extends JFrame {

    private JTextField  jrxmlPathField;
    private JTextArea  jsonInput ;
    private String jsonData = "";
    private String choice = "PDF";

    public JrxmlUI(){
        setTitle("JRXML Local Tester UI");
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout(10, 10));

        // --- Top Panel: Drag & Drop zone for JRXML ---
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("JRXML Template (Drag & Drop file here)"));

        jrxmlPathField = new JTextField();
        jrxmlPathField.setEditable(false);
        topPanel.add(jrxmlPathField, BorderLayout.CENTER);

       //options
       //
       String options[] = {"PDF", "DOC", "EXCEL"};
       JComboBox<String> box = new JComboBox<>(options);

       box.setSelectedItem("PDF");

       box.addActionListener(e -> {
           choice = (String) box.getSelectedItem();
           System.out.println(choice);
       });


        // --- Center Panel: JSON Payload Input ---
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBorder(BorderFactory.createTitledBorder("JSON Data Payload"));

        jsonInput = new JTextArea("");
        centerPanel.add(new JScrollPane(jsonInput), BorderLayout.CENTER);


        // Enable Drag & Drop for the text field
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();

            // Optional: Restrict selection to .jrxml files
            fileChooser.setFileFilter(new FileNameExtensionFilter("JRXML Files (*.jrxml)", "jrxml"));

            int returnValue = fileChooser.showOpenDialog(this);
            if (returnValue == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                jrxmlPathField.setText(selectedFile.getAbsolutePath());
                //map from jrxml to json input
                Map<String, String> jsonMap = extractFields( selectedFile, this);
                List rowList = new ArrayList<>();
                rowList.add(jsonMap);
                try {
                ObjectMapper  neededField = new ObjectMapper();
                jsonData = neededField.writerWithDefaultPrettyPrinter().writeValueAsString(rowList);

                jsonInput.setText(jsonData);

                } catch (Exception ee) {
                	ee.printStackTrace();
                }

            }
        });

        topPanel.add(jrxmlPathField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);


        // --- Bottom Panel: Action Button ---
        JPanel bottomPanel = new JPanel();
        JButton generateButton = new JButton("Generate Report PDF");
        generateButton.addActionListener(e -> generateReport(this));
        bottomPanel.add(generateButton);
        bottomPanel.add(box);

        // Add to main frame
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void generateReport(Component parentComponent){
        System.out.println("about to generate Report........");
        String jrxmlPath = jrxmlPathField.getText();
        String jsonPayload = jsonInput.getText();
        System.out.println(jsonPayload);
        if (!jrxmlPath.isEmpty()) {
            try {
                jrxmlViewer(jrxmlPath, jsonPayload, parentComponent);

                } catch (Exception e) {
                  showErroMessage( e, parentComponent);
                }
            }

        }

    public  void jrxmlViewer(String p1, String jsonContent, Component parentComponent) throws Exception{
       System.out.println("Compiling JRXML");
       JasperReport jasperReport = JasperCompileManager.compileReport(p1);

       System.out.println("Parsing JSON payload...");
      //dev/ String jsonContent = new String(Files.readAllBytes(new File(p2).toPath()));
       ObjectMapper mapper = new ObjectMapper();
       List  jsonData = mapper.readValue(jsonContent  , List.class );


       JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(jsonData);

       Map <String, Object> parameters = new HashMap<>();
       parameters.put("ReportTitle", "Local Test Run");


       System.out.println("Filling report ...");
       JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);


       File exportDir = new File("exports");
       if(!exportDir.exists()){
           exportDir.mkdirs();
       }
       
       String jrxmlFileName = Paths.get(p1).getFileName().toString();
       String extractFileName[] = jrxmlFileName.split("\\.");
       String outputPath = "exports/"+extractFileName[0]+"_output";
       System.out.println("Exporting to pdf: " + new String(outputPath+".pdf"));

       switch (choice ) {
       	case "PDF":
                JasperExportManager.exportReportToPdfFile(jasperPrint, new String(outputPath+".pdf") );
       		break;
            case "DOC":
                JRDocxExporter exporter = new JRDocxExporter();
                exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
                exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(new String(outputPath + ".docx")));

                SimpleDocxExporterConfiguration configuration = new SimpleDocxExporterConfiguration();

                exporter.setConfiguration(configuration);
                exporter.exportReport();

           

                break;
            case "EXCEL":
                JRXlsxExporter export = new JRXlsxExporter();
                export.setExporterInput(new SimpleExporterInput(jasperPrint));
                export.setExporterOutput(new SimpleOutputStreamExporterOutput(outputPath+".xlsx"));
                SimpleXlsxReportConfiguration configurationDoc =
                                    new SimpleXlsxReportConfiguration();
                configurationDoc.setOnePagePerSheet(false);
                configurationDoc.setDetectCellType(true);
                configurationDoc.setRemoveEmptySpaceBetweenRows(true);
                configurationDoc.setRemoveEmptySpaceBetweenColumns(true);
                export.setConfiguration(configurationDoc);
                export.exportReport();
                break;
                default:
                    break;
            }


       System.out.println("Done! repot generated successfully.");

       // 2. Give option to open the file when done generating
        int choice = JOptionPane.showConfirmDialog(
            parentComponent,
            "Report generated successfully!\nWould you like to open it now?",
            "Success",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            File pdfFile = new File(outputPath);
            if (pdfFile.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
            }
        }
    }

    public Map<String, String> extractFields(File file, Component parentComponent){

        Map<String,String> map = new HashMap<>();

        try {

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder() ;
            Document   doc = dBuilder.parse(file);
            doc.getDocumentElement().normalize();

            NodeList  nList = doc.getElementsByTagName("field");


            for(int temp = 0; temp < nList.getLength(); temp++){

                Node  nNode =nList.item(temp);
                if(nNode.getNodeType()  == Node.ELEMENT_NODE){
                    Element eElement = (Element )nNode;
                    String fieldName = eElement.getAttribute("name");
                    String fieldClass = eElement.getAttribute( "class");
                    map.put(fieldName , fieldClass);
                }
            }

        } catch (Exception e) {
            showErroMessage(e, parentComponent);
        }

        return map;
    }


    public void showErroMessage(Exception  e, Component parentComponent){
        JOptionPane.showMessageDialog(
            parentComponent,
            "Error generating report:\n" + e.getMessage(),
            "Execution Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

}
