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
    boolean raw = false;
    boolean verbose = false;
    if ( args.length > 0 ) {
    	if ( args[0].equals( "html" ) ) {
    		raw = true;
    	} else if ( args[0].equals( "standard" ) ) {
    		raw = false;
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
    s.processFolder( s.root, raw, verbose );
    System.out.println("Done.");
  }
  
  private void processFolder( File file, boolean raw, boolean verbose ) throws IOException
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
        for ( int i = 0; i < afile.length; i++ )
        {
          if ( afile[i].isDirectory() )
          {
            //System.out.println( "Directory: " + afile[i].getName() );
            this.processFolder( afile[i], raw, verbose );
          } else if ( raw && afile[i].getName().equals( "footnote.js" ) )
          {
            //System.out.println( "File: " + afile[i].getName() );
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
          } else if ( !raw && afile[i].getName().equals( "footnote_[original].js" ) )
          {
            File nf = new File( afile[i].getParentFile(), "footnote.js" );
            if ( nf.exists() ) nf.delete();
            boolean r = afile[i].renameTo( nf );
            if ( !r )
            {
              System.out.println( "FAILED TO RESTORE: " + afile[i].getAbsolutePath() );
              return;
            }
          } else
          {
            // Ignore other files
          }
        }
      }
    } else
    {
      //System.out.println( "(file: " + file.getName() + ")");
    }
  }

  private void copyCustomJS( Path path ) throws IOException
  {
    System.out.println("copying: " + this.customJS.toPath() + " to " + path);
    Files.copy( this.customJS.toPath(), path );
  }
}
