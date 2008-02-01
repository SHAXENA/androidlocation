/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.hipoqih.plugin.Web;

import com.hipoqih.plugin.UI.*;
import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import com.hipoqih.plugin.*;

/**
 *
 * @author Fernando
 */
public class Mapa extends Thread {
    private MainFormUI mForm;
    private String mUrl;

    //CONSTRUCTOR
    //*****************************************
    //LE PASO COMO PARAMETROS EL FORM QUE LO LLAMO, Y LA
    //URL A BUSCAR
    //*****************************************
    public Mapa(MainFormUI mForm, String mUrl) {
        System.out.println(mUrl);
        this.mForm = mForm;
        this.mUrl = mUrl;
    }
    
    //PARTE QUE EJECUTA CUANDO SE STARTEA EL HILO
    public void run () {
        HttpConnection c = null;
        InputStream is = null;
        Image image = null;

        try {
            //CREO LA CONEXION
            c = (HttpConnection)Connector.open(this.mUrl);        
            int rc = c.getResponseCode();
            if (rc != HttpConnection.HTTP_OK )
            {
                    throw new IOException("Error HTTP:" + rc);
            }
            // Obtenemos el "stream" de datos
            is = c.openInputStream();

            image = Image.createImage(is);

        } 
        catch(Exception ex)
        {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
        }
        System.out.println("aca");
        
        //LLAMO A displayMap EN EL HILO PRINCIPAL PARA MOSTRAR LA IMAGEN
        this.mForm.displayMap(image);
    }
    
}
