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
public class LogosPrep {

	/**
	 * 
	 */
	public LogosPrep() 
	{
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException 
	{

    	LogosPrep lp = new LogosPrep();
	    if ( args[0].endsWith( ".html" ) ) {
	    	lp.prepareFile( args[0] );
	      } else {
	        File d = new File( args[0] );
	        if (!d.isDirectory()) System.err.println("'" + args[0] + "' is not a directory.");
	        String[] subs = d.list();
	        for (int i = 0; i < subs.length; i++) {
	          if ( subs[i].endsWith( ".html" ) ) lp.prepareFile( d.getPath() + File.separator + subs[i] );
	        }
	      }
	}

	private void prepareFile( String file ) throws IOException
	{
		File input = new File( file );
		Document doc = Jsoup.parse(input, "UTF-8");
		
		/*
		 * Check for a specific type of tag that marks commentary verse
		 * For 21st Century Biblical Commentary Series:
		 * FONT-SIZE: 0.95em; FONT-STYLE: italic; TEXT-ALIGN: center; MARGIN: 15px 10%; LINE-HEIGHT: 1.4em; TEXT-INDENT: 0px
		 * 
		 * For Spurgeon (sermon heading):
		 * MARGIN-BOTTOM: 0px; FONT-SIZE: 1.5em; FONT-WEIGHT: bold; TEXT-ALIGN: center; MARGIN-TOP: 0.5em; LINE-HEIGHT: normal 
		 */
		int btags = 0;
		Elements ps = null;
		Elements links = null;
		
		/*
		 * Parse raw HTML output from WS
		 */
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
                        span.attr("class").equalsIgnoreCase("trans-arc") ||
                        span.attr("class").equalsIgnoreCase("gloss")     ||
                        span.attr("class").equalsIgnoreCase("lang-heb")  ||
                        span.attr("class").equalsIgnoreCase("lang-lat")  ||
                        span.attr("class").equalsIgnoreCase("lang-ger") ) {
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
                btags++;
              }
			} else {
			  String cls = span.attr( "class" );
			  if ( !missed.contains( cls ) ) missed.add( cls );
			}
		}
		if (btags > 0 ) System.out.println(btags + " commentary tags inserted.");
        if ( missed.size() > 0 ) System.out.println("  SPAN classes not handled: " + missed.toString() );
		
		btags = 0;
		Elements headers = doc.getElementsByTag("h1");
	    for ( Element header : headers )
	    {
	      header.before( "{{field-on:heading}}" );
	      header.after( "{{field-off:heading}}" );
	      btags++;
	    }
	    if (btags > 0 ) System.out.println( btags + " (H1) headings tagged.");
		
        btags = 0;
        headers = doc.getElementsByTag("h2");
        for ( Element header : headers )
        {
          header.before( "{{field-on:heading}}" );
          header.after( "{{field-off:heading}}" );
          btags++;
        }
        if (btags > 0 ) System.out.println( btags + " (H2) headings tagged.");

        
        /*
         * Check for IMG base URL
         */
        btags = 0;
        String imgpath = "";
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
        
        if ( imgpath == null ) {
          System.out.println("Image base path is NULL!");
        } else {
          Elements imgs = doc.getElementsByTag("img");
          for ( Element img : imgs )
          {
            String rel = img.attr( "src" );
            img.attr( "src", imgpath + rel );
            System.out.println("   IMAGE: " + img.attr( "src" ));
            btags++;
          }
          if (btags > 0 ) System.out.println( btags + " images converted.");
        }

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
	
	private String getLogosRef( String title )
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
	
	private String getBibleRef( String href ) throws MalformedURLException, UnsupportedEncodingException
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
	
	private boolean isBibleRef( String ref )
	{
		String[] a = {"genesis", "gen", "ge", "gn", "exodus", "exo", "ex", "exod", "leviticus", "lev", "le", "lv", "numbers", "num", "nu", "nm", "nb", "deuteronomy", "deut", "dt", "joshua", "josh", "jos", "jsh", "judges", "judg", "jdg", "jg", "jdgs", "ruth", "rth", "ru", "1 samuel", "1 sam", "1 sa", "1samuel", "1s", "i sa", "1 sm", "1sa", "i sam", 
				"1sam", "i samuel", "1st samuel", "first samuel", "2 samuel", "2 sam", "2 sa", "2s", "ii sa", "2 sm", "2sa", "ii sam", "2sam", "ii samuel", "2samuel", "2nd samuel", "second samuel", "1 kings", "1 kgs", "1 ki", "1k", "i kgs", "1kgs", "i ki", "1ki", "i kings", "1kings", "1st kgs", "1st kings", "first kings", "first kgs", "1kin", "2 kings", "2 kgs", "2 ki", "2k", "ii kgs", "2kgs", "ii ki", "2ki", "ii kings", 
				"2kings", "2nd kgs", "2nd kings", "second kings", "second kgs", "2kin", "1 chronicles", "1 chron", "1 ch", "i ch", "1ch", "1 chr", "i chr", "1chr", "i chron", "1chron", "i chronicles", "1chronicles", "1st chronicles", "first chronicles", "2 chronicles", "2 chron", "2 ch", "ii ch", "2ch", "ii chr", "2chr", "ii chron", "2chron", "ii chronicles", "2chronicles", "2nd chronicles", "second chronicles", "ezra", "ezr", "nehemiah", "neh", "ne", "esther", "esth", "es", 
				"job", "jb", "psalm", "pslm", "ps", "psalms", "psa", "psm", "pss", "proverbs", "prov", "pr", "prv", "pro", "ecclesiastes", "eccles", "ec", "ecc", "qoh", "qoheleth", "song of solomon", "song", "so", "canticle of canticles", "canticles", "song of songs", "sos", "isaiah", "isa", "is", "jeremiah", "jer", "je", "jr", "lamentations", "lam", "la", "ezekiel", "ezek", "eze", "ezk", 
				"daniel", "dan", "da", "dn", "hosea", "hos", "ho", "joel", "joe", "jl", "amos", "am", "obadiah", "obad", "ob", "jonah", "jnh", "jon", "micah", "mic", "nahum", "nah", "na", "habakkuk", "hab", "zephaniah", "zeph", "zep", "zp", "haggai", "hag", "hg", "zechariah", "zech", "zec", "zc", "malachi", "mal", "ml", "matthew", "matt", 
				"mt", "mark", "mrk", "mk", "mr", "luke", "luk", "lk", "john", "jn", "jhn", "acts", "ac", "romans", "rom", "ro", "rm", "1 corinthians", "1 cor", "1 co", "i co", "1co", "i cor", "1cor", "i corinthians", "1corinthians", "1st corinthians", "first corinthians", "2 corinthians", "2 cor", "2 co", "ii co", "2co", "ii cor", "2cor", "ii corinthians", "2corinthians", "2nd corinthians", "second corinthians", "galatians", "gal", 
				"ga", "ephesians", "ephes", "eph", "philippians", "phil", "php", "colossians", "col", "1 thessalonians", "1 thess", "1 th", "i th", "1th", "i thes", "1thes", "i thess", "1thess", "i thessalonians", "1thessalonians", "1st thessalonians", "first thessalonians", "2 thessalonians", "2 thess", "2 th", "ii th", "2th", "ii thes", "2thes", "ii thess", "2thess", "ii thessalonians", "2thessalonians", "2nd thessalonians", "second thessalonians", "1 timothy", "1 tim", "1 ti", "i ti", "1ti", "i tim", 
				"1tim", "i timothy", "1timothy", "1st timothy", "first timothy", "2 timothy", "2 tim", "2 ti", "ii ti", "2ti", "ii tim", "2tim", "ii timothy", "2timothy", "2nd timothy", "second timothy", "titus", "tit", "ts", "philemon", "philem", "phm", "hebrews", "heb", "james", "jas", "jm", "ja", "1 peter", "1 pet", "1 pe", "i pe", "1pe", "i pet", "1pet", "i pt", "1 pt", "1pt", "i peter", "1peter", "1st peter", 
				"first peter", "2 peter", "2 pet", "2 pe", "ii pe", "2pe", "ii pet", "2pet", "ii pt", "2 pt", "2pt", "ii peter", "2peter", "2nd peter", "second peter", "1 john", "1 jn", "i jn", "1jn", "i jo", "1jo", "i joh", "1joh", "i jhn", "1 jhn", "1jhn", "i john", "1john", "1st john", "first john", "2 john", "2 jn", "ii jn", "2jn", "ii jo", "2jo", "ii joh", "2joh", "ii jhn", "2 jhn", "2jhn", 
				"ii john", "2john", "2nd john", "second john", "3 john", "3 jn", "iii jn", "3jn", "iii jo", "3jo", "iii joh", "3joh", "iii jhn", "3 jhn", "3jhn", "iii john", "3john", "3rd john", "third john", "jude", "jud", "revelation", "rev", "re"};
		for (int i = 0; i < a.length; i++) {
			int l = a[i].length();
			if ( ref.length() > l ) {
				if ( a[i].equalsIgnoreCase(ref.substring(0, l))) return true;				
			}
		}
		return false;
	}
	
	private class Sermon
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
