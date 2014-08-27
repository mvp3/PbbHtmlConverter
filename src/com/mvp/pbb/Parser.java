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
 * This is both the entry class to run a command-line program
 * and the base class for creating Parser sub-classes.
 */

/* 
 * Copyright (c) 2014 Manuel Pereira.
 * 895 Oak Brook Way, Gilroy, California, 95020, U.S.A.
 * All rights reserved.
 *
 * Created on July 28, 2014
 */
public class Parser
{

	/**
	 * 
	 */
	public Parser() 
	{
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws Exception 
	{
		System.out.println("Starting PBB parser version 1.4");

		Parser lp = null;
		String a = args[0];
		
	    if ( a.equalsIgnoreCase( "ws" ) ) {
	    	lp = new Parser();
	    } else if ( a.equalsIgnoreCase( "trans" ) ) {
	    	lp = new TranslatedHTMLParser();
	    } else if ( a.equalsIgnoreCase( "spurgeon" ) ) {
	    	lp = new SpurgeonParser();
	    } else if ( a.equalsIgnoreCase( "kindle" ) ) {
	    	lp = new KindleParser();
	    } else {
	    	Parser.invalidArgument();
	    	return;
	    }

	    a = args[1];
	    if ( a == null || lp == null ) Parser.invalidArgument();
	    
	    if ( a.endsWith( ".html" ) ) {
	    	lp.prepareFile( a );
	      } else {
	        File d = new File( a );
	        if (!d.isDirectory()) System.err.println("'" + a + "' is not a directory.");
	        String[] subs = d.list();
	        for (int i = 0; i < subs.length; i++) {
	          if ( subs[i].endsWith( ".html" ) ) lp.prepareFile( d.getPath() + File.separator + subs[i] );
	        }
	      }
	}
	
	private static void invalidArgument()
	{
    	System.out.println("Invalid argument: com.mvp.pbb.Parser <type> <file/folder>");
    	System.out.println("    Parser types: ws, trans, spurgeon, kindle");
    	System.out.println("         Example: com.mvp.pbb.Parser ws \"Meyer's Daily Devotional.html\"");
	}

	void prepareFile( String file ) throws Exception
	{
		File input = new File( file );
		Document doc = Jsoup.parse(input, "UTF-8");
		
		/*
		 * Process Headers H1, H2, and H3
		 */
		convertHeaders(doc);
		
		/*
		 * Generic method for special processing
		 */
		processDoc(doc);
		
		/*
		 * Process all <SPAN> tags
		 */
		convertAllSpans(doc);
		
		/*
		 * Process all <P> tags
		 */
		convertAllParagraphs(doc);
        
        /*
         * Process tables
         */
        convertAllTables(doc);
        
        /*
         * Check and convert all images
         */
        convertAllImages(doc);

        /*
		 * Check all links and convert them 
		 */
        convertAllLinks(doc);

		/*
		 * Write the modified HTML file back to the file system with a [modified] tag in the file name.
		 */
        writeHTMLFile(input, doc);
	}
	
	public void processDoc( Document doc ) throws Exception
	{
		// Do nothing.
	}
	
	/**
	 * Tag all H1 headings and demote headings that match
	 * "Preface" and "Introduction"
	 * 
	 * @param doc
	 */
	public void convertHeaders( Document doc ) throws Exception
	{
		int tags = 0;
		Elements headers = doc.getElementsByTag("h1");
	    for ( Element header : headers )
	    {
	    	if (header.text().equalsIgnoreCase("preface") ||
	    		header.text().equalsIgnoreCase("introduction") ) {
	    		header.tagName("h2");
	    	} else {
		    	header.before( "{{field-on:heading}}" );
		    	header.after( "{{field-off:heading}}" );
		    	tags++;
	    	}
	    }
	    if (tags > 0 ) System.out.println( tags + " (H1) headings tagged.");

	    tags = 0;
        headers = doc.getElementsByTag("h2");
        for ( Element header : headers )
        {
	    	if (header.text().equalsIgnoreCase("preface") ||
		    	header.text().equalsIgnoreCase("introduction") ) {
	    		// Skip
	    	} else {
		    	header.before( "{{field-on:heading}}" );
		    	header.after( "{{field-off:heading}}" );
	        	tags++;
	    	}
        }
        if (tags > 0 ) System.out.println( tags + " (H2) headings tagged.");
	}
	
	/**
	 * 
	 * @param doc
	 */
	public void convertAllSpans( Document doc ) throws Exception
	{
		int tags = 0;
		ArrayList<String> missed = new ArrayList<String>();
		Elements spans = doc.getElementsByTag("span");
		for ( Element span : spans ) {
			if ( span.attr("class").equalsIgnoreCase("esPageNumber") ) {
			  String pn = span.text();
			  span.replaceWith(new TextNode("[[@Page:" + pn + "]]", ""));
			} else if ( span.attr("class").equalsIgnoreCase("fn") ) {
			  span.remove();
			} else if ( span.attr("class").equalsIgnoreCase("fntext") ) {
			  String txt = span.text();
			  span.replaceWith(new TextNode("{{@footnote:" + txt + "}}", ""));
				
            } else if ( span.attr("class").equalsIgnoreCase("trans-grc") || 
                        span.attr("class").equalsIgnoreCase("trans-heb") ||
                        span.attr("class").startsWith("trans-") 		 ||
                        span.attr("class").equalsIgnoreCase("gloss")     ||
                        span.attr("class").startsWith("lang-") ) {
              String txt = span.text();
              Element e = new Element(Tag.valueOf("i"), "");
              e.text(txt);
              span.replaceWith(e);
              
            } else if ( span.attr("class").equalsIgnoreCase("smcaps") ) {
              span.attr( "class", "" );
              span.attr( "style", "font-variant:small-caps;" );
              
            /* For Twenty-First Century Biblical Commentary Series */
            } else if ( span.attr("class").equalsIgnoreCase("scripture") ) {
              String txt = span.attr("value");
              if ( txt != null && txt.length() > 1 ) {
                span.replaceWith(new TextNode("[[@Bible:" + txt + "]]", ""));
                tags++;
              }
			} else {
			  String cls = span.attr( "class" );
			  if ( !missed.contains( cls ) ) missed.add( cls );
			}
		}
		if (tags > 0 ) System.out.println(tags + " commentary tags inserted.");
        if ( missed.size() > 0 ) System.out.println("  <SPAN> classes not handled: " + missed.toString() );
	}
	
	/**
	 * 
	 * @param doc
	 * @throws UnsupportedEncodingException 
	 * @throws MalformedURLException 
	 */
	public void convertAllParagraphs( Document doc ) throws Exception
	{
        int tags = 0;
        ArrayList<String> missed = new ArrayList<String>();
        Elements ps = doc.getElementsByTag("p");
		for ( Element p : ps ) {
			if ( p.attr("class").equalsIgnoreCase("booktitle") ) {
				String txt = p.text();
	            Element e = new Element(Tag.valueOf("h1"), "");
	            e.text(txt);
	            e.attr("id", "title");
	            p.replaceWith(e);
	            e.before("{{field-on:title}}");
	            e.after("{{field-off:title}}");
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("subtitle") ) {
				String txt = p.text();
	            Element e = new Element(Tag.valueOf("h1"), "");
	            e.text(txt);
	            e.attr( "id", "subtitle" );
            	e.attr( "style", "font-size:smaller;" );
	            p.replaceWith(e);
	            e.before("{{field-on:subtitle}}");
	            e.after("{{field-off:subtitle}}");
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("byline") ) {
            	p.attr( "style", "text-align:center;" );
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("author") ) {
	            p.attr( "id", "author" );
            	p.attr( "style", "text-align:center;font-size:large;font-weight:bold;" );
	            p.before("{{field-on:author}}");
	            p.after("{{field-off:author}}");
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("ded1") || p.attr("class").equalsIgnoreCase("ded2") ) {
	            p.attr( "id", "dedication" );
            	p.attr( "style", "font-style:italic;text-align:center;line-height:0.3;" );
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("copyright") ) {
            	p.attr( "style", "font-size:small;text-align:center;" );
	            p.before("{{field-on:copyright}}");
	            p.after("{{field-off:copyright}}");
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("caption") ) {
            	p.attr( "style", "font-size:small;text-align:center;" );
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("center") ) {
            	p.attr( "style", "text-align:center;" );
	            tags++;
			} else if ( p.attr("class").equalsIgnoreCase("publogo") ) {
            	p.attr( "style", "text-align:center;" );
	            tags++;
            } else if ( p.attr("class").equalsIgnoreCase("wslogo") || 
                        p.attr("class").equalsIgnoreCase("coverimg") ) {
            	p.remove();
	            tags++;
            } else if ( p.attr("class").equalsIgnoreCase("noind") ) {
            	p.attr( "class", "" );
            	p.attr( "style", "" );
            	tags++;
            } else if ( p.attr("class").equalsIgnoreCase("sig")  ||
        				p.attr("class").equalsIgnoreCase("sig2") || 
        				p.attr("class").equalsIgnoreCase("sig3") ) {
            	p.attr( "class", "signature" );
            	p.attr( "style", "text-align:right;font-style:italic;line-height:0.3;" );
	            tags++;
            } else if ( p.attr("class").equalsIgnoreCase("map")  ||
        				p.attr("class").equalsIgnoreCase("mapcenter") || 
        				p.attr("class").equalsIgnoreCase("chart") ) {
            	p.attr( "style", "text-align:center;" );
	            tags++;
            } else if ( p.attr("class").equalsIgnoreCase("hang") ||
        				p.attr("class").equalsIgnoreCase("hang2") ) {
	        	p.attr( "style", "padding-left:22px; text-indent:-22px;" );
	        	tags++;
            } else if ( p.attr("class").equalsIgnoreCase("poem1") ||
            			p.attr("class").equalsIgnoreCase("poem2") || 
            			p.attr("class").equalsIgnoreCase("poem3") ||
            			p.attr("class").equalsIgnoreCase("poem4")) {
            	p.attr( "class", "poem" );
            	p.attr( "style", "font-style:italic;text-indent:22px;line-height:0.3;" );
            	tags++;
			} else {
				String cls = p.attr( "class" );
				if ( !missed.contains( cls ) ) missed.add( cls );
			}
		}
		if (tags > 0 ) System.out.println(tags + " paragraphs processed.");
        if ( missed.size() > 0 ) System.out.println("  <P> classes not handled: " + missed.toString() );
	}
	
	/**
	 * Make sure that tables are properly formatted
	 * 
	 * @param doc
	 */
	public void convertAllTables( Document doc ) throws Exception
	{
		Elements tables = doc.getElementsByTag("table");
		if ( tables.size() > 0 ) {
            Element e = new Element(Tag.valueOf("style"), "");
            e.text("tr.nowrap { white-space:nowrap; } td { white-space:nowrap; }");
			doc.body().before(e);
		}
	}
	/**
	 * Check for an IMG base URL and insert image base URL into each IMG tag.
	 * 
	 * @param doc
	 */
	public void convertAllImages( Document doc ) throws Exception
	{
        /*
         * Check for IMG base URL
         */
        /*
        Elements inputs = doc.getElementsByTag("input");
        for ( Element inp : inputs )
        {
          if ( inp.attr( "name" ).equalsIgnoreCase( "imgdata" ) && inp.attr( "value" ).length() > 0 ) {
            imgpath = inp.attr( "value" );
            System.out.println("IMG: " + imgpath);
            imgpath = imgpath.substring( 0, imgpath.indexOf( "Linked" ) );
            break;
          }
        }
        */
		int tags = 0;
        String imgpath = "file:///C:/Books/WORDsearch/Library/WelwynCmy/";
        if ( imgpath == null ) {
          System.out.println("Image base path is NULL!");
        } else {
          Elements imgs = doc.getElementsByTag("img");
          for ( Element img : imgs )
          {
            String rel = img.attr( "src" );
            img.attr( "src", imgpath + this.getImgDirWelwyn(rel) + "/" + rel );
            //System.out.println("   IMAGE: " + img.attr( "src" ));
            tags++;
          }
          if (tags > 0 ) System.out.println( tags + " images converted.");
        }
	}
	
	/**
	 * (1) Remove all HTML links set to Bible references, 
	 * (2) convert all links of page numbers to PBB page tags,
	 * (3) remove all "essm" links   
	 * 
	 * @param doc
	 * @throws UnsupportedEncodingException
	 */
	public void convertAllLinks( Document doc ) throws Exception 
	{
		Elements links = doc.getElementsByTag("a");
		for (Element link : links) {
			if (isBibleRef(link.text())) {
				link.replaceWith(new TextNode(link.text(), ""));
			} else {
				if ( !link.attr("name").isEmpty() ) {
					String a = link.attr("name"); 
					if ( a.startsWith("page") ) {
						String page = a.substring(4);
						link.replaceWith(new TextNode("[[@Page:" + page + "]]", ""));
					} else if ( a.equals("essm") ) {
						link.remove();
					}
				} else if ( !link.attr("href").isEmpty() ) {
				  try {
                    String n = getBibleRef(link.attr("href"));
                    if ( n != null ) {
                        link.text( n );
                        //System.out.println("  - Changing " + o + " to " + n);
                        link.replaceWith(new TextNode(link.text(), ""));
                    }
				  } catch ( MalformedURLException ex ) {
				    System.out.println("Removing link to: " + link.attr( "href" ));
				    link.replaceWith(new TextNode(link.text(), ""));
				  }
				}
			}
		}
		
	}
	
	/**
	 * Write the modified HTML file back to the file system with a [modified] tag in the file name.
	 * 
	 * @param file File in the local file system to write
	 * @param doc HTML Document object that contains processed HTML
	 * @throws IOException
	 */
	public void writeHTMLFile( File file, Document doc ) throws IOException
	{
		String fn = file.getAbsolutePath();
		File output = new File( fn.substring(0, fn.lastIndexOf('.')) + "_[modified].html");
		BufferedWriter bw = new BufferedWriter(new FileWriter(output));
		bw.write(doc.toString());
		bw.close();

		System.out.println("Converted file: " + output.getName() );
        System.out.println("Original file size: " + file.length() );
        System.out.println("Converted file size: " + output.length() );
	}
	
	String getImgDirWelwyn( String img )
	{
		String series = "Welwyn";
		String str = img.substring(img.lastIndexOf('/') + 1);
		str = str.substring(0, str.indexOf('.'));
		int dash = str.indexOf('-');
		if ( dash > 2 ) str = str.substring(0, dash);
		int w = str.indexOf(series);
		if ( w == -1 ) {
			if ( str.equalsIgnoreCase("EvangelicalPress") || 
				 str.equalsIgnoreCase("wslogo") ||
				 str.equalsIgnoreCase("numberscoverimg") ||
				 str.equalsIgnoreCase("coverimage") ) {
				str = "Rev";
			} else if ( str.startsWith("2-2Samuel") ) {
				str = "2Samuel";
			}
		} else if ( w == 0 ) {
			str = str.substring(series.length());
		} else {
			str = str.substring(0, w);
		}
		//System.out.println("  (imgDir: " + str + ")");
		return str;
	}

	public String getLogosRef( String title )
	{
		/*
		 * 	logosres:pbb:8dad9c2cf121434cbc6dfc8fb403b878;art=a$5F0098-makinglightofchrist
		 *	logosres:pbb:8dad9c2cf121434cbc6dfc8fb403b878;art=a$5F0098a-theexaltationofchrist
		 */
		
		int del = title.indexOf('-');
		String num = title.substring(0, del).trim();
		String name = title.substring(del).trim().toLowerCase();
		return "logosres:pbb:8dad9c2cf121434cbc6dfc8fb403b878;art=a$5F" + num + name.replaceAll("\\s+", "").replaceAll("\\'+",  "");
	}
	
	public String getBibleRef( String href ) throws MalformedURLException, UnsupportedEncodingException
	{
		try {
			URL u = new URL( href );
			String r = u.getQuery();
			if ( r == null ) return null;
			String[] a = r.split("=");
			if ( a.length > 2 ) return null;
			return URLDecoder.decode( a[1], "UTF-8" );
		} catch ( MalformedURLException ex ) {
			if ( !href.startsWith("logos") ) throw ex; 
			return null;
		} catch ( Exception ex ) {
			System.err.println("Error in getBibleRef::" + ex.getMessage());
			System.out.println("Invalid URL: " + href);
			return null;
		}
	}
	
	public boolean isBibleRef( String ref )
	{
		String[] a = {"genesis", "gen", "ge", "gn", "exodus", "exo", "ex", "exod", "leviticus", "lev", "le", "lv", "numbers", "num", "nu", "nm", "nb", "deuteronomy", "deut", "dt", "joshua", "josh", "jos", "jsh", "judges", "judg", "jdg", "jg", "jdgs", "ruth", "rth", "ru", "1 samuel", "1 sam", "1 sa", "1samuel", "1s", "i sa", "1 sm", "1sa", "i sam", 
				"1sam", "i samuel", "1st samuel", "first samuel", "2 samuel", "2 sam", "2 sa", "2s", "ii sa", "2 sm", "2sa", "ii sam", "2sam", "ii samuel", "2samuel", "2nd samuel", "second samuel", "1 kings", "1 kgs", "1 ki", "1k", "i kgs", "1kgs", "i ki", "1ki", "i kings", "1kings", "1st kgs", "1st kings", "first kings", "first kgs", "1kin", "2 kings", "2 kgs", "2 ki", "2k", "ii kgs", "2kgs", "ii ki", "2ki", "ii kings", 
				"2kings", "2nd kgs", "2nd kings", "second kings", "second kgs", "2kin", "1 chronicles", "1 chron", "1 ch", "i ch", "1ch", "1 chr", "i chr", "1chr", "i chron", "1chron", "i chronicles", "1chronicles", "1st chronicles", "first chronicles", "2 chronicles", "2 chron", "2 ch", "ii ch", "2ch", "ii chr", "2chr", "ii chron", "2chron", "ii chronicles", "2chronicles", "2nd chronicles", "second chronicles", "ezra", "ezr", "nehemiah", "neh", "ne", "esther", "esth", "es", 
				"job", "jb", "psalm", "pslm", "ps", "psalms", "psa", "psm", "pss", "proverbs", "prov", "pr", "prv", "pro", "ecclesiastes", "eccles", "ec", "ecc", "qoh", "qoheleth", "song of solomon", "song", "so", "canticle of canticles", "canticles", "song of songs", "ss", "sos", "isaiah", "isa", "is", "jeremiah", "jer", "je", "jr", "lamentations", "lam", "la", "ezekiel", "ezek", "eze", "ezk", 
				"daniel", "dan", "da", "dn", "hosea", "hos", "ho", "joel", "joe", "jl", "amos", "am", "obadiah", "obad", "ob", "jonah", "jnh", "jon", "micah", "mic", "nahum", "nah", "na", "habakkuk", "hab", "zephaniah", "zeph", "zep", "zp", "haggai", "hag", "hg", "zechariah", "zech", "zec", "zc", "malachi", "mal", "ml", "matthew", "matt", 
				"mt", "mark", "mrk", "mk", "mr", "luke", "luk", "lk", "john", "jn", "jhn", "acts", "ac", "romans", "rom", "ro", "rm", "1 corinthians", "1 cor", "1 co", "i co", "1co", "i cor", "1cor", "i corinthians", "1corinthians", "1st corinthians", "first corinthians", "2 corinthians", "2 cor", "2 co", "ii co", "2co", "ii cor", "2cor", "ii corinthians", "2corinthians", "2nd corinthians", "second corinthians", "galatians", "gal", 
				"ga", "ephesians", "ephes", "eph", "philippians", "phil", "php", "colossians", "col", "1 thessalonians", "1 thess", "1 th", "i th", "1th", "i thes", "1thes", "i thess", "1thess", "i thessalonians", "1thessalonians", "1st thessalonians", "first thessalonians", "2 thessalonians", "2 thess", "2 th", "ii th", "2th", "ii thes", "2thes", "ii thess", "2thess", "ii thessalonians", "2thessalonians", "2nd thessalonians", "second thessalonians", "1 timothy", "1 tim", "1 ti", "i ti", "1ti", "i tim", 
				"1tim", "i timothy", "1timothy", "1st timothy", "first timothy", "2 timothy", "2 tim", "2 ti", "ii ti", "2ti", "ii tim", "2tim", "ii timothy", "2timothy", "2nd timothy", "second timothy", "titus", "tit", "ts", "philemon", "philem", "phm", "hebrews", "heb", "james", "jas", "jm", "ja", "1 peter", "1 pet", "1 pe", "i pe", "1pe", "i pet", "1pet", "i pt", "1 pt", "1pt", "i peter", "1peter", "1st peter", 
				"first peter", "2 peter", "2 pet", "2 pe", "ii pe", "2pe", "ii pet", "2pet", "ii pt", "2 pt", "2pt", "ii peter", "2peter", "2nd peter", "second peter", "1 john", "1 jn", "i jn", "1jn", "i jo", "1jo", "i joh", "1joh", "i jhn", "1 jhn", "1jhn", "i john", "1john", "1st john", "first john", "2 john", "2 jn", "ii jn", "2jn", "ii jo", "2jo", "ii joh", "2joh", "ii jhn", "2 jhn", "2jhn", 
				"ii john", "2john", "2nd john", "second john", "3 john", "3 jn", "iii jn", "3jn", "iii jo", "3jo", "iii joh", "3joh", "iii jhn", "3 jhn", "3jhn", "iii john", "3john", "3rd john", "third john", "jude", "jud", "revelation", "rev", "re"};
		if ( ref == null ) return false;
		for (int i = 0; i < a.length; i++) {
			int l = a[i].length();
			if ( ref.length() > l ) {
				if ( a[i].equalsIgnoreCase(ref.substring(0, l))) return true;				
			}
		}
		return false;
	}
	
	public class Sermon
	{
		String title = null;
		String venue = null;
		String date = null;
		String passages = null;
		String topics = null;
		String tags = null;
		String speaker = "Charles H. Spurgeon";
		ArrayList<String> references = new ArrayList<String>();

		String getInfoTable()
		{
			StringBuffer s = new StringBuffer();
			s.append("<table><tr><td>Sermon File Type</td><td>Sermon</td></tr>\n");
			if ( this.passages != null ) {
				s.append("<tr><td>Passages</td><td>");
				s.append(this.passages);
				s.append("</td></tr>\n");
			}
			if ( this.topics != null ) {
				s.append("<tr><td>Topics</td><td>");
				s.append(this.topics);
				s.append("</td></tr>\n");
			}
			if ( this.tags != null ) {
				s.append("<tr><td>Tags</td><td>");
				s.append(this.tags);
				s.append("</td></tr>\n");
			}
			if ( this.date != null ) {
				s.append("<tr><td>Date</td><td>");
				s.append(this.date);
				s.append("</td></tr>\n");
			}
			if ( this.speaker != null ) {
				s.append("<tr><td>Speakers</td><td>");
				s.append(this.speaker);
				s.append("</td></tr>\n");
			}
			if ( this.venue != null ) {
				s.append("<tr><td>Venues</td><td>");
				s.append(this.venue);
				s.append("</td></tr>\n");
			}
			if ( this.references.size() > 0 ) {
				s.append("<tr><td>References</td><td>");
				for ( String r : this.references ) {
					s.append( r );
					s.append("; ");
				}
				s.append("</td></tr>\n");
			}
			s.append("</table>\n");
			this.refresh();
			return s.toString();
		}
		void refresh()
		{
			this.title = null;
			this.venue = null;
			this.date = null;
			this.passages = null;
			this.topics = null;
			this.tags = null;
			this.speaker = "Charles H. Spurgeon";
			this.references.clear();
		}
	}
}
