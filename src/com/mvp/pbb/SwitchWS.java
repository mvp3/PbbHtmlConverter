package com.mvp.pbb;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/* 
 * Copyright (c) 2005 Manuel Pereira.
 * 895 Oak Brook Way, Gilroy, California, 95020, U.S.A.
 * All rights reserved.
 *
 * Created on Aug 1, 2014
 */

public class SwitchWS
{
  private File root = null;
  private File customJS = null;
  private int depth = 0;
  private boolean cancel = false;
  
  public SwitchWS( File root )
  {
    this.root = root;
    this.customJS = new File( this.root, "custom_footnote.js" );
  }
  
  public static void main( String[] args ) throws IOException
  {
    boolean html = false;
    boolean verbose = false;
    if ( args.length > 0 ) {
    	if ( args[0].equals( "html" ) || args[0].equals( "raw" ) || args[0].equals( "h" ) ) {
    		html = true;
    	} else if ( args[0].equals( "standard" ) 	||
    				args[0].equals( "std" ) 		||
    				args[0].equals( "reset" ) 		||
    				args[0].equals( "s" ) ) {
    		html = false;
    	} else {
            System.out.println("Invalid option. Options: sw html [or] sw standard");
            return;
    	}
        if ( args.length > 1 ) {
        	if ( args[1].equals( "-v" ) ) {
        		verbose = true;
        	}
        }
    	
    } else {
        System.out.println("Invalid option. Options: sw html [or] sw standard");
        return;
    }
    SwitchWS s = new SwitchWS(new File("C:\\Books\\WORDsearch\\Library"));
    s.processFolder( s.root, html, verbose );
    System.out.println("Done.");
  }
  
  private void processFolder( File file, boolean html, boolean verbose ) throws IOException
  {
    if ( cancel ) return;
    if ( file.isDirectory() )
    {
      /*
      if ( file.getName().equals( "1000EvangIllus" ) ) {
        System.out.println("Reached last folder!");
        this.cancel = true;
        return;
      }
      */
      File afile[] = file.listFiles();
      if ( verbose ) System.out.println( "Reading directory: " + file.getName() );
      if ( afile.length > 0 )
      {
    	boolean nojs = true;
        for ( int i = 0; i < afile.length; i++ )
        {
          if ( afile[i].isDirectory() )
          {
            //System.out.println( "Directory: " + afile[i].getName() );
            this.processFolder( afile[i], html, verbose );
          }
          else if ( html && afile[i].getName().equals( "footnote.js" ) )
          {
            //System.out.println( "File: " + afile[i].getName() );
        	nojs = false;
            Path path = afile[i].toPath();
            String fn = afile[i].getAbsolutePath();
            File nf = new File( fn.substring( 0, fn.lastIndexOf( '.' ) ) + "_[original].js" );
            if ( nf.exists() )
            {
              // Avoid overwriting original: go straight to copy
              afile[i].delete();
              this.copyCustomJS( path );
            } else
            {
              System.out.println( "Renaming: " + afile[i].getAbsolutePath() );
              if ( nf.exists() ) nf.delete();
              boolean r = afile[i].renameTo( nf );
              if ( !r )
              {
                System.out.println( "FAILED TO RENAME: " + afile[i].getAbsolutePath() );
                return;
              }
              this.copyCustomJS( path );
            }
          }
          else if ( !html && afile[i].getName().equals( "footnote_[original].js" ) )
          {
          	nojs = false;
            File nf = new File( afile[i].getParentFile(), "footnote.js" );
            if ( nf.exists() ) nf.delete();
            boolean r = afile[i].renameTo( nf );
            if ( !r )
            {
              System.out.println( "FAILED TO RESTORE: " + afile[i].getAbsolutePath() );
              return;
            }
          }
          else if ( afile[i].getName().endsWith(".js") )
          {
        	nojs = false;
          }
          else
          {
            // Ignore other files
          }
        }
        /*
         * If a "Linked" directory does not contain a JavaScript file, 
         * then (a) create an empty file called "footnote_[original].js"
         * and (b) copy the custom JS file to "footnote.js". 
         */
        if ( file.getName().equals("Linked") && nojs ) {
            File ef = new File( file, "footnote_[original].js" );
            ef.createNewFile();
            File nf = new File( file, "footnote.js" );
        	this.copyCustomJS( nf.toPath() );
        }
      }
    } else
    {
      //System.out.println( "(file: " + file.getName() + ")");
    }
  }

  private void copyCustomJS( Path path ) throws IOException
  {
    System.out.println("Copying custom JS to: " + path);
    Files.copy( this.customJS.toPath(), path );
  }
}
