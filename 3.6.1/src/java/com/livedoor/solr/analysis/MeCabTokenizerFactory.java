package com.livedoor.solr.analysis;

import com.livedoor.lucene.analysis.MeCabTokenizer;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.solr.analysis.BaseTokenizerFactory;
import org.apache.solr.common.ResourceLoader;
import org.apache.solr.common.util.StrUtils;
import org.apache.solr.util.plugin.ResourceLoaderAware;

/**
 * Creates new instances of MeCabTokenizer.
 */
public class MeCabTokenizerFactory extends BaseTokenizerFactory implements ResourceLoaderAware
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

    public static final String ARG_STOP_POSIDS = "stopPosids";
    public static final String ARG_MECAB_ARGS = "mecabArgs";
    public static final String ARG_TERM_TYPE = "termType";
    public static final String ARG_TERM_FALLBACK_TYPE = "termFallbackType";
    public static final String TERM_TYPE_SURFACE = "surface";
    public static final String TERM_TYPE_FEATURE = "feature";
    public static final String TERM_TYPE_FORMATTED = "formatted";
    public static final String ARG_TYPE_AS = "typeAs";
    public static final String ARG_PAYLOAD_AS = "payloadAs";
    public static final String AS_POSID = "posid";
    public static final String AS_SURFACE = "surface";
    public static final String AS_FEATURE = "feature";
    public static final String AS_FORMATTED = "formatted";

    private String mecabArgs = null;
    private List<Integer> stopPosids = null;
    private MeCabTokenizer.TermType termType = MeCabTokenizer.TermType.SURFACE;
    private MeCabTokenizer.TermType termFallbackType = MeCabTokenizer.TermType.SURFACE;
    private MeCabTokenizer.TypeAs typeAs = MeCabTokenizer.TypeAs.NONE;
    private MeCabTokenizer.PayloadAs payloadAs = MeCabTokenizer.PayloadAs.NONE;

    /** ResourceLoaderAware */
    @Override
    public void inform(ResourceLoader loader)
    {
        mecabArgs = args.get(ARG_MECAB_ARGS);
        String stopPosidsFileStr = args.get(ARG_STOP_POSIDS);
        if (stopPosidsFileStr != null)
        {
            try
            {
                File stopPosidsFile = new File(stopPosidsFileStr);
                if (stopPosidsFile.exists())
                {
                    List<String> list = loader.getLines(stopPosidsFileStr);
                    stopPosids = new ArrayList<Integer>();
                    Iterator<String> it = list.iterator();
                    while (it.hasNext())
                    {
                        String posid = it.next();
                        stopPosids.add(Integer.valueOf(posid));
                    }
                }
                else
                {
                    List<String> stopPosidsFilesStr = StrUtils.splitFileNames(stopPosidsFileStr);
                    for (String stopPosidsFileStr2 : stopPosidsFilesStr)
                    {
                        List<String> list = loader.getLines(stopPosidsFileStr2.trim());
                        if (stopPosids == null)
                        {
                            stopPosids = new ArrayList<Integer>();
                        }
                        Iterator<String> it = list.iterator();
                        while (it.hasNext())
                        {
                            String posid = it.next();
                            stopPosids.add(Integer.valueOf(posid));
                        }
                    }
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        String termTypeOpt = args.get(ARG_TERM_TYPE);
        if (TERM_TYPE_FEATURE.equals(termTypeOpt))
        {
            termType = MeCabTokenizer.TermType.FEATURE;
        }
        else if (TERM_TYPE_FORMATTED.equals(termTypeOpt))
        {
            termType = MeCabTokenizer.TermType.FORMATTED;
        }
        else
        {
            termType = MeCabTokenizer.TermType.SURFACE;
        }
        String termFallbackTypeOpt = args.get(ARG_TERM_FALLBACK_TYPE);
        if (TERM_TYPE_FEATURE.equals(termFallbackTypeOpt))
        {
            termFallbackType = MeCabTokenizer.TermType.FEATURE;
        }
        else
        {
            termFallbackType = MeCabTokenizer.TermType.SURFACE;
        }
        String typeAsOpt = args.get(ARG_TYPE_AS);
        if (AS_POSID.equals(typeAsOpt))
        {
            typeAs = MeCabTokenizer.TypeAs.POSID;
        }
        else if (AS_SURFACE.equals(typeAsOpt))
        {
            typeAs = MeCabTokenizer.TypeAs.SURFACE;
        }
        else if (AS_FEATURE.equals(typeAsOpt))
        {
            typeAs = MeCabTokenizer.TypeAs.FEATURE;
        }
        else if (AS_FORMATTED.equals(typeAsOpt))
        {
            typeAs = MeCabTokenizer.TypeAs.FORMATTED;
        }
        else
        {
            typeAs = MeCabTokenizer.TypeAs.NONE;
        }
        String payloadAsOpt = args.get(ARG_PAYLOAD_AS);
        if (AS_POSID.equals(payloadAsOpt))
        {
            payloadAs = MeCabTokenizer.PayloadAs.POSID;
        }
        else if (AS_SURFACE.equals(payloadAsOpt))
        {
            payloadAs = MeCabTokenizer.PayloadAs.SURFACE;
        }
        else if (AS_FEATURE.equals(payloadAsOpt))
        {
            payloadAs = MeCabTokenizer.PayloadAs.FEATURE;
        }
        else if (AS_FORMATTED.equals(payloadAsOpt))
        {
            payloadAs = MeCabTokenizer.PayloadAs.FORMATTED;
        }
        else
        {
            payloadAs = MeCabTokenizer.PayloadAs.NONE;
        }
    }

    /** Creates the TokenStream of terms tokenized by the MeCabTokenizer from the given Reader. */
    @Override
    public Tokenizer create(Reader input)
    {
        MeCabTokenizer t = new MeCabTokenizer(input, mecabArgs);
        t.setStopPosids(stopPosids);
        t.setTermType(termType);
        t.setTermFallbackType(termFallbackType);
        t.setTypeAs(typeAs);
        t.setPayloadAs(payloadAs);
        return t;
    }
}

