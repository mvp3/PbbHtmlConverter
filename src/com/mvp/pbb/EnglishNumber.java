package com.mvp.pbb;

public class EnglishNumber {

	final private static String[] tens = {"","","twenty","thirty","forty","fifty", 
		"sixty","seventy","eighty","ninety"};
	
	final private static String[] numbers = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", 
		"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", 
		"nineteen", "twenty", "twenty-one", "twenty-two", "twenty-three", "twenty-four", "twenty-five", 
		"twenty-six", "twenty-seven", "twenty-eight", "twenty-nine", "thirty", "thirty-one", "thirty-two", 
		"thirty-three", "thirty-four", "thirty-five", "thirty-six", "thirty-seven", "thirty-eight", 
		"thirty-nine", "forty", "forty-one", "forty-two", "forty-three", "forty-four", "forty-five", 
		"forty-six", "forty-seven", "forty-eight", "forty-nine", "fifty", "fifty-one", "fifty-two", 
		"fifty-three", "fifty-four", "fifty-five", "fifty-six", "fifty-seven", "fifty-eight", "fifty-nine", 
		"sixty", "sixty-one", "sixty-two", "sixty-three", "sixty-four", "sixty-five", "sixty-six", 
		"sixty-seven", "sixty-eight", "sixty-nine", "seventy", "seventy-one", "seventy-two", 
		"seventy-three", "seventy-four", "seventy-five", "seventy-six", "seventy-seven", 
		"seventy-eight", "seventy-nine", "eighty", "eighty-one", "eighty-two", "eighty-three", 
		"eighty-four", "eighty-five", "eighty-six", "eighty-seven", "eighty-eight", "eighty-nine", 
		"ninety", "ninety-one", "ninety-two", "ninety-three", "ninety-four", "ninety-five", 
		"ninety-six", "ninety-seven", "ninety-eight", "ninety-nine"};

	public EnglishNumber() 
	{
	}

	public static String getEnglish( int i ) 
	{
		if( i < 20)  return numbers[i];
		if( i < 100) return tens[i/10] + ((i % 10 > 0)? "-" + getEnglish(i % 10):"");
		if( i < 1000) return numbers[i/100] + " Hundred" + ((i % 100 > 0)?" and " + getEnglish(i % 100):"");
		if( i < 1000000) return getEnglish(i / 1000) + " Thousand " + ((i % 1000 > 0)? " " + getEnglish(i % 1000):"") ;
		return getEnglish(i / 1000000) + " Million " + ((i % 1000000 > 0)? " " + getEnglish(i % 1000000):"") ;
	}
	
	public static String getEnglish( Integer i ) 
	{
		return getEnglish( i.intValue() );
	}

	/**
	 * Returns the alpha-numeric value of the English number provided.
	 * 
	 * @param num - English word (i.e. one, twenty-three, forty). 
	 * 				If num is an alpha-numeric, the same value will be returned.  
	 * @return Alpha-numeric value in a String (i.e. 1, 23, 40). 
	 */
	public static String getNumberString( String num )
	{
		try {
			return Integer.valueOf(num).toString();
		} catch ( NumberFormatException nex ) {
			for ( int i = 0; i < numbers.length; i++ ) {
				if ( num.equalsIgnoreCase(numbers[i]) ) {
					return String.valueOf(i);
				}
			}
			throw nex;
		}
	}
	
	public static int getNumber( String num )
	{
		return Integer.valueOf(getNumberString( num )).intValue();
	}
	
}
