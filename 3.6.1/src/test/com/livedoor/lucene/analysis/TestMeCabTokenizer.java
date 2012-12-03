package com.livedoor.lucene.analysis;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.util._TestUtil;

/**
 * Tests for {@link MeCabTokenizer}
 */
public class TestMeCabTokenizer extends BaseTokenStreamTestCase
{
    static
    {
        try
        {
            System.loadLibrary("MeCab");
        }
        catch (Exception e)
        {
            System.err.println("loading MeCab library failed.\n" + e);
        }
    }

    private Analyzer analyzerDefault = new ReusableAnalyzerBase()
    {
        @Override
        protected TokenStreamComponents createComponents(String field, Reader reader)
        {
            MeCabTokenizer tokenizer = new MeCabTokenizer(reader);
            return new TokenStreamComponents(tokenizer, tokenizer);
        };
    };

    private Analyzer analyzerFeature = new ReusableAnalyzerBase()
    {
        @Override
        protected TokenStreamComponents createComponents(String field, Reader reader)
        {
            MeCabTokenizer tokenizer = new MeCabTokenizer(reader);
            tokenizer.setTermType(MeCabTokenizer.TermType.FEATURE);
            return new TokenStreamComponents(tokenizer, tokenizer);
        };
    };

    private Analyzer analyzerFormatted = new ReusableAnalyzerBase()
    {
        @Override
        protected TokenStreamComponents createComponents(String field, Reader reader)
        {
            MeCabTokenizer tokenizer = new MeCabTokenizer(reader);
            tokenizer.setTermType(MeCabTokenizer.TermType.FORMATTED);
            tokenizer.setMecabArgs("-F %f[7]\\0 -U %M\\0");
            return new TokenStreamComponents(tokenizer, tokenizer);
        };
    };

    public void testDecomposition1() throws IOException
    {
        assertAnalyzesTo(
            analyzerDefault,
            "これはMeCabTokenizerのテストです。",
            new String[] { "これ", "は", "MeCabTokenizer", "の", "テスト", "です", "。" },
            new int[] {         0,    2,                3,   17,       18,     21,   23 },
            new int[] {         2,    3,               17,   18,       21,     23,   24 }
        );
    }

    public void testAlphabeticAndNumeric() throws IOException
    {
        assertAnalyzesToReuse(
            analyzerDefault,
            "012 ABC 012ABC 012-345-678 012ABC",
            new String[] { "012", "ABC", "012", "ABC", "012", "-", "345", "-", "678", "012", "ABC" },
            new int[] {        0,     4,     8,    11,    15,  18,    19,  22,    23,    27,    30 },
            new int[] {        3,     7,    11,    14,    18,  19,    22,  23,    26,    30,    33 }
        );
    }

    public void testFeature() throws IOException
    {
        assertAnalyzesToReuse(
            analyzerFeature,
            "テスト",
            new String[] { "名詞,サ変接続,*,*,*,*,テスト,テスト,テスト" },
            new int[] {                                               0 },
            new int[] {                                               3 }
        );
    }

    public void testFormatted() throws IOException
    {
        assertAnalyzesToReuse(
            analyzerFormatted,
            "太郎と花子とホゲホゲです。",
            new String[] { "タロウ", "ト", "ハナコ", "ト", "ホゲホゲ", "デス", "。" },
            new int[] {           0,    2,      3,      5,          6,    10,    12 },
            new int[] {           2,    3,      5,      6,         10,    12,    13 }
        );
    }

    public void testReliability() throws IOException
    {
        for (int i = 0; i < 10000; i++) {
            String s = _TestUtil.randomUnicodeString(random, 100);
            TokenStream ts = analyzerDefault.reusableTokenStream("foo", new StringReader(s));
            ts.reset();
            while (ts.incrementToken());
        }
    }
}

