package org.svearike.trainingpalweb;

import java.util.HashMap;
import java.util.Map;

public class URLPathParameters
{
	private Map<String, String> mMappedValues = new HashMap<>();
	private final boolean mValid;
	private String mError = null;

	public URLPathParameters(String format, String input)
	{
		if (input == null || format == null) {
			mError = "Input=" + input + ", Format=" + format;
			mValid = false;
			return;
		}

		String[] formatSplit = format.split("/");
		String[] inputSplit = input.split("/");
		if (formatSplit.length != inputSplit.length) {
			mError = "formatSplit.length(" + formatSplit.length + ") != inputSplit.length(" + inputSplit.length + ")";
			mValid = false;
			return;
		}

		for(int i=0;i<formatSplit.length;i++)
		{
			String fmt = formatSplit[i];
			String val = inputSplit[i];
			if (fmt.startsWith("${"))
			{
				String name = fmt.replaceAll("[${}]", "");
				mMappedValues.put(name, val);
			}
			else if (!fmt.equals(val))
			{
				mError = "Expected " + fmt + " got " + val;
				mValid = false;
				return;
			}
		}

		mValid = true;
	}

	public boolean isValid()
	{
		return mValid;
	}

	public String getError()
	{
		return mError;
	}

	@Override
	public String toString() {
		return mMappedValues.toString();
	}

	public String getString(String key) {
		return mMappedValues.get(key);
	}

	public int getInt(String key) {
		return Integer.parseInt(getString(key));
	}

	public long getLong(String key) {
		return Long.parseLong(getString(key));
	}
}
