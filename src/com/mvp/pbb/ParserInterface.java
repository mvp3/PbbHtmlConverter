package com.mvp.pbb;

import java.io.UnsupportedEncodingException;

import org.jsoup.nodes.Document;

public interface ParserInterface 
{
	/*
	 * Generic process method
	 */
	public void processDoc( Document doc ) throws Exception;
	
	/*
	 * Process all headers
	 */
	public void convertHeaders( Document doc ) throws Exception;
	
	/*
	 * Process all <SPAN> tags
	 */
	public void convertAllSpans( Document doc ) throws Exception;
	
	/*
	 * Process all <P> tags
	 */
	public void convertAllParagraphs( Document doc ) throws Exception;
    
    /*
     * Process tables
     */
	public void convertAllTables( Document doc ) throws Exception;
    
    /*
     * Check and convert all images
     */
	public void convertAllImages( Document doc ) throws Exception;

    /*
	 * Check all links and convert them 
	 */
	public void convertAllLinks( Document doc ) throws Exception;

}
