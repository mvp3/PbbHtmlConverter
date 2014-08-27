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

/* 
 * Copyright (c) 2014 Manuel Pereira.
 * 895 Oak Brook Way, Gilroy, California, 95020, U.S.A.
 * All rights reserved.
 *
 * Created on July 28, 2014
 */
public class SpurgeonParser extends Parser implements ParserInterface
{

	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#processDoc(org.jsoup.nodes.Document)
	 */
	@Override
	public void processDoc(Document doc) throws Exception 
	{
		super.processDoc(doc);
	}


	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertH1Headers(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertHeaders(Document doc) throws Exception 
	{
		super.convertHeaders(doc);
	}

	
	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllSpans(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllSpans(Document doc) throws Exception
	{
		super.convertAllSpans(doc);
	}


	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllParagraphs(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllParagraphs(Document doc) throws Exception
	{
		super.convertAllParagraphs(doc);
	}


	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllTables(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllTables(Document doc) throws Exception
	{
		super.convertAllTables(doc);
	}


	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllImages(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllImages(Document doc) throws Exception 
	{
		super.convertAllImages(doc);
	}


	/* (non-Javadoc)
	 * @see com.mvp.pbb.Parser#convertAllLinks(org.jsoup.nodes.Document)
	 */
	@Override
	public void convertAllLinks(Document doc) throws Exception
	{
		super.convertAllLinks(doc);
	}


	/**
	 * 
	 */
	SpurgeonParser() 
	{
	}


	void prepareFile( String file ) throws IOException
	{
		File input = new File( file );
		Document doc = Jsoup.parse(input, "UTF-8");
		
		/*
		 * Check for a specific type of tag that marks commentary verse
		 * For Spurgeon (sermon heading):
		 * MARGIN-BOTTOM: 0px; FONT-SIZE: 1.5em; FONT-WEIGHT: bold; TEXT-ALIGN: center; MARGIN-TOP: 0.5em; LINE-HEIGHT: normal 
		 */
		boolean spurgeon = true;
		boolean tfc = false;
		
		int btags = 0;
		Elements ps = doc.getElementsByTag("p");
		Elements links = null;
		
		/*
		 * 21st Century Biblical Commentary Series
		 */
		if ( tfc ) {
			for (Element p : ps) {
				if (!p.attr("style").isEmpty()) {
					if ( p.attr("style").equalsIgnoreCase("FONT-SIZE: 0.95em; FONT-STYLE: italic; TEXT-ALIGN: center; MARGIN: 15px 10%; LINE-HEIGHT: 1.4em; TEXT-INDENT: 0px") ) {
						p.prepend("[[@Bible:" + p.text() + "]]");
						btags++;
					}
				}
			}
		}
		
		/*
		 * Charles H. Spurgeon
		 */
		if ( spurgeon ) {
			Elements hrs = doc.getElementsByTag("hr");
			for (Element hr : hrs) {
				hr.remove();
			}
			
			Sermon s = new Sermon();
			for ( int i = 0; i < ps.size(); i++ ) {
				Element p = ps.get(i);
				String t = p.text();
				if (!p.attr("style").isEmpty()) {
					if ( p.text().endsWith("Index") ) {
						p.remove();
						                                       // 
					} else if ( p.attr("style").equalsIgnoreCase("MARGIN-BOTTOM: 0px; FONT-SIZE: 1.5em; FONT-WEIGHT: bold; TEXT-ALIGN: center; MARGIN-TOP: 0.5em; LINE-HEIGHT: normal") ||
							(p.attr("style").equalsIgnoreCase("MARGIN-BOTTOM: 0px; FONT-SIZE: 1em; TEXT-ALIGN: center; MARGIN-TOP: 0.5em; LINE-HEIGHT: normal") && p.child(0).attr("style").equalsIgnoreCase("FONT-SIZE: 1.5em")) ) {
						// A new title
						Element e = new Element(Tag.valueOf("h1"), "");
						e.text(t);
						p.replaceWith(e);
						s.title = t;
						
						//Check for references
						p = ps.get(++i);
						t = p.text();
						if ( t.equalsIgnoreCase("see also:") ) {
							p = ps.get(++i);
							t = p.text();
							for ( ; i < ps.size(); i++) {
								p = ps.get(i);
								t = p.text();
								Elements a = p.getElementsByTag("a");
								if ( a == null || a.size() == 0 ) break;
								Element link = a.get(0);
								link.attr("href", this.getLogosRef(link.text()));
								s.references.add(link.toString());
							}
						}
						if ( t.length() > 0 && !t.substring(0, 1).matches("[0-9]") ) {
							s.venue = t;
							p.remove();
							p = ps.get(++i);
							t = p.text();
						}
						if ( t.length() > 0 && t.substring(0, 1).matches("[0-9]") ) {
							s.date = t;
							p.remove();
							p = ps.get(++i);
							t = p.text();
						}
						boolean found = false;
						if ( p.html().equals("&nbsp;") ) {
							int c = 0;
							for ( ; i < ps.size(); i++) {
								p = ps.get(i);
								links = p.getElementsByTag("a");
								if ( links != null && links.size() > 0 ) {
									Element a = links.get(0);
									t = a.text();
									if ( this.isBibleRef(t) ) {
										a.replaceWith(new TextNode("[[@Bible:" + t + "]]" + t, ""));
										s.passages = t;
										btags++;
										Element br = new Element(Tag.valueOf("br"), ""); 
										p.after(br);
										p.after(br);
										found = true;
										break;
									}
								}
								if ( c++ > 4 ) break;;
							}
						}
						if ( !found ) System.out.println("NO PASSAGE FOR: " + s.title);

						
						// Print out information table
						Element ne = new Element(Tag.valueOf("table"), "");
						ne.html(s.getInfoTable());
						e.after( ne );
					}
				}
			}
			Elements tables = doc.getElementsByTag("table");
			for (Element table : tables ) {
				if ( table.attr("style").length() > 40 ) {
					table.remove();
				}
			}
		}
		if (btags > 0 ) System.out.println(btags + " commentary tags inserted.");
		
		
		/*
		 * Check all links and convert them 
		 */
		links = doc.getElementsByTag("a");
		for (Element link : links) {
			if (isBibleRef(link.text())) {
				link.replaceWith(new TextNode(link.text(), ""));
			} else {
				String o = link.text();
				if ( !link.attr("name").isEmpty() ) {
					String a = link.attr("name"); 
					if ( a.startsWith("page") ) {
						String page = a.substring(4);
						link.replaceWith(new TextNode("[[@Page:" + page + "]]", ""));
					}
				} else if ( !link.attr("href").isEmpty() ) {
					String n = getBibleRef(link.attr("href"));
					if ( n != null ) {
						link.text( n );
						//System.out.println("  - Changing " + o + " to " + n);
						link.replaceWith(new TextNode(link.text(), ""));
					}
				}
			}
		}
		
		/*
		 * Write the modified HTML file back to the file system with a [modified] tag in the file name.
		 * All non-content lines will be excluded. 
		 */
		String fn = input.getAbsolutePath();
		File output = new File( fn.substring(0, fn.lastIndexOf('.')) + "_[modified].html");
		FileWriter fw = new FileWriter(output);
		String[] lines = doc.toString().split("\r\n|\r|\n");
		String nl = System.getProperty("line.separator");
		int excluded = 0;

		for (int i = 0; i < lines.length; i++) {
			String s = lines[i].trim(); 
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
        System.out.println("Original file size: " + input.length() );
        System.out.println("Converted file size: " + output.length() );
	}
	
}
