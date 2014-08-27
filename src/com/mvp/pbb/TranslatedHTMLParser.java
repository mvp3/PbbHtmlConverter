package com.mvp.pbb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

/**
 * 
 */

@SuppressWarnings("unused")
/* 
 * Copyright (c) 2014 Manuel Pereira.
 * 895 Oak Brook Way, Gilroy, California, 95020, U.S.A.
 * All rights reserved.
 *
 * Created on July 28, 2014
 */
class TranslatedHTMLParser extends Parser implements ParserInterface
{

	/**
	 * 
	 */
	TranslatedHTMLParser() 
	{
	}

	public void processDoc( Document doc ) throws Exception
	{
	    /*
	     * Attempt to Tag chapter headings that correspond to Bible chapters
	     */
		int btags = 0;
		String book = "";
		Elements elements = doc.getAllElements();
	    for ( int i = 0; i < elements.size(); i++ )
	    {
	    	Element e = elements.get(i);
	    	if ( e.tagName().equalsIgnoreCase("p") && e.attr("style").startsWith("FONT-SIZE: 2.1em") ) {
	    		book = e.text();
	    		Element h1 = new Element(Tag.valueOf("h1"), "");
	    		h1.text(book);
	    		e.replaceWith(h1);
	    	} else if ( e.tagName().equalsIgnoreCase("h2") ) {
	    		String ct = "Chapter ";
	    		String t = e.text();
	    		if ( t.startsWith(ct) ) {
	    			Element ne = elements.get(i + 1);
	    			if ( ne.tagName().equalsIgnoreCase("p") && ne.attr("style").equals("FONT-SIZE: 0.95em; FONT-WEIGHT: bold; TEXT-ALIGN: center; MARGIN: 10px 10%; LINE-HEIGHT: normal; TEXT-INDENT: 0px") ) {
	    				// Ignore
	    			} else {
		    			int ei = t.indexOf('.');
		    			if ( ei == -1 ) ei = t.indexOf("<BR>");
		    			if ( ei == -1 ) ei = t.length();
		    			String cn = t.substring(ct.length(), ei);
		    			if ( cn != null && !cn.isEmpty() ) {
		    				try {
		    					if ( cn.indexOf(':') > 0 && cn.indexOf(':') < 4 ) {
		    						// Ignore if the string contains a colon, assuming it is a Bible reference
		    					} else {
			    					cn = EnglishNumber.getNumberString(cn);
		    					}
			    				e.before(new TextNode("[[@Bible:" + book + " " + cn + "]]", ""));
			    				btags++;
		    				} catch ( NumberFormatException nfe ) {
		    					// Ignore
		    				}
		    			}
	    			}
	    		}
	    	}
	    }
	    if (btags > 0 ) System.out.println( btags + " chapters tagged as Bible milestones.");
		
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertH1Headers(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertHeaders(final Document doc) throws Exception 
	{
	
        int btags = 0;
		Elements headers = doc.getElementsByTag("h5");
	    for ( final Element header : headers )
	    {
	    	header.tagName("h6");
	    	btags++;
	    }
	    if (btags > 0 ) System.out.println( btags + " (H5->6) headings tagged.");

        btags = 0;
		headers = doc.getElementsByTag("h4");
	    for ( final Element header : headers )
	    {
	    	header.tagName("h5");
	    	btags++;
	    }
	    if (btags > 0 ) System.out.println( btags + " (H4->5) headings tagged.");

        btags = 0;
		headers = doc.getElementsByTag("h3");
	    for ( final Element header : headers )
	    {
	    	header.tagName("h4");
	    	btags++;
	    }
	    if (btags > 0 ) System.out.println( btags + " (H3->4) headings tagged.");


        btags = 0;
        headers = doc.getElementsByTag("h2");
        for ( final Element header : headers )
        {
    		final String t = header.text();
    		header.tagName("h3");
        	
	    	if (t.equalsIgnoreCase("preface") ||
		    	t.equalsIgnoreCase("introduction") ) {
	    		// Skip
	    	} else {
	    		if ( this.isBibleRef(t) ) {
					header.before(new TextNode("[[@Bible:" + t + "]]", ""));
	    		}
		    	header.before( "{{field-on:heading}}" );
		    	header.after( "{{field-off:heading}}" );
	        	btags++;
	    	}
        }
        if (btags > 0 ) System.out.println( btags + " (H2->3) headings tagged.");

        btags = 0;
		headers = doc.getElementsByTag("h1");
	    for ( final Element header : headers )
	    {
	    	if (header.text().equalsIgnoreCase("preface") ||
	    		header.text().equalsIgnoreCase("introduction") ) {
	    		header.tagName("h3");
	    	} else {
		    	header.before( "{{field-on:heading}}" );
		    	header.after( "{{field-off:heading}}" );
	    		header.tagName("h2");
		    	btags++;
	    	}
	    }
	    if (btags > 0 ) System.out.println( btags + " (H1->2) headings tagged.");
		
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllSpans(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllSpans(final Document doc) throws Exception 
	{
		/*
		 * Parse translated HTML output from WS
		 */
		int btags = 0;
		final ArrayList<String> missed = new ArrayList<String>();
		final Elements spans = doc.getElementsByTag("span");
		for ( final Element span : spans ) {
			if ( span.attr("class").equalsIgnoreCase("esPageNumber") ) {
			  final String pn = span.text();
			  span.replaceWith(new TextNode("[[@Page:" + pn + "]]", ""));
			} else if ( span.attr("class").equalsIgnoreCase("fn") ) {
			  span.remove();
			} else if ( span.attr("class").equalsIgnoreCase("fntext") ) {
			  final String txt = span.text();
			  span.replaceWith(new TextNode("{{@footnote:" + txt + "}}", ""));
				
            } else if ( span.attr("class").equalsIgnoreCase("trans-grc") || 
	                    span.attr("class").equalsIgnoreCase("trans-heb") ||
	                    span.attr("class").startsWith("trans-") 		 ||
	                    span.attr("class").equalsIgnoreCase("gloss")     ||
	                    span.attr("class").startsWith("lang-") ) {
	          final String txt = span.text();
	          final Element e = new Element(Tag.valueOf("i"), "");
	          e.text(txt);
	          span.replaceWith(e);
              
            } else if ( span.attr("class").equalsIgnoreCase("smcaps") ) {
              span.attr( "class", "" );
              span.attr( "style", "font-variant:small-caps;" );
              
            /* For Twenty-First Century Biblical Commentary Series */
            } else if ( span.attr("class").equalsIgnoreCase("scripture") ) {
              final String txt = span.attr("value");
              if ( txt != null && txt.length() > 1 ) {
                span.replaceWith(new TextNode("[[@Bible:" + txt + "]]", ""));
                btags++;
              }
			} else {
			  final String cls = span.attr( "class" );
			  if ( !missed.contains( cls ) ) missed.add( cls );
			}
		}
		if (btags > 0 ) System.out.println(btags + " commentary tags inserted.");
        if ( missed.size() > 0 ) System.out.println("  SPAN classes not handled: " + missed.toString() );
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllParagraphs(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllParagraphs(final Document doc) throws Exception 
	{
		int btags = 0;
		final Elements paragraphs = doc.getElementsByTag("p");
	    for ( final Element p : paragraphs )
	    {
	    	if ( p.attr("style").equals("FONT-SIZE: 0.95em; FONT-WEIGHT: bold; TEXT-ALIGN: center; MARGIN: 10px 10%; LINE-HEIGHT: normal; TEXT-INDENT: 0px") ) {
	    		final Elements links = p.getElementsByTag("a");
	    		if ( links != null && links.size() > 0 ) {
					final Element a = links.get(0);
					final String t = getBibleRef(a.attr("href"));
					if ( this.isBibleRef(t) ) {
						a.replaceWith(new TextNode("[[@Bible:" + t + "]] " + a.text(), ""));
				    	btags++;
					} else {
						System.out.println("  (WARNING: '" + t + "' not a Bible reference)");
					}
	    		}
	    	}
	    }
	    if (btags > 0 ) System.out.println( btags + " references tagged as Bible milestones.");
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllTables(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllTables(final Document doc) throws Exception 
	{
		super.convertAllTables(doc);
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllImages(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllImages(final Document doc) throws Exception 
	{
        /*
		 * Convert all Images 
		 */
	    int btags = 0;
		final Elements imgs = doc.getElementsByTag("img");
		for (final Element img : imgs) {
			if ( img.attr("alt").equalsIgnoreCase("cover image") || img.attr("alt").startsWith("WORDsearch") ) {
				img.remove();
			} else {
				String t = img.attr("src");
				t = t.replace('\\', '/');
				t = "file:///" + t;
				img.attr("src", t);
				btags++;
			}
		}
	    if (btags > 0 ) System.out.println( btags + " images converted.");
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllLinks(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllLinks(final Document doc) throws Exception
	{
		super.convertAllLinks(doc);
	}

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#writeHTMLFile(java.io.File, org.jsoup.nodes.Document)
	 */
	@Override
	public void writeHTMLFile(final File file, final Document doc) throws IOException 
	{
		/*
		 * Write the modified HTML file back to the file system with a [modified] tag in the file name.
		 * All non-content lines will be excluded. 
		 */
		final String fn = file.getAbsolutePath();
		final File output = new File( fn.substring(0, fn.lastIndexOf('.')) + "_[modified].html");
		final FileWriter fw = new FileWriter(output);
		final String[] lines = doc.toString().split("\r\n|\r|\n");
		final String nl = System.getProperty("line.separator");
		int excluded = 0;

		for (int i = 0; i < lines.length; i++) {
			final String s = lines[i].trim(); 
			if ( s.startsWith("Version:1.0 StartHTML:") || s.startsWith("<!--EndFragment-->") ) {
				//System.out.println( i + ": " + lines[i] );
				excluded++;
			} else {
				fw.write( lines[i] );
				fw.write( nl );
			}
		}
        System.out.println("Excluded " + excluded + " lines.");
		
        fw.close();
        System.out.println("Converted file: " + output.getName() );
        System.out.println("Original file size: " + file.length() );
        System.out.println("Converted file size: " + output.length() );
	}
	
}
