package com.app;

import javax.swing.SwingUtilities;


public class App 
{
    public static void main( String[] args )
    {

        SwingUtilities.invokeLater(()-> new JrxmlUI().setVisible( true));
        try {
           //jrxmlViewer(jrxmlPath, jsonPath);
        } catch (Exception e) {
           System.out.println("error"+ e.getMessage() );
        }

    }

}
