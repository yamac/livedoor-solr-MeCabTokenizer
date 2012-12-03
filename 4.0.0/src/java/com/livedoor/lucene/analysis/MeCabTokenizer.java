package com.livedoor.lucene.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.BytesRef;
import org.chasen.mecab.MeCabConstants;
import org.chasen.mecab.Node;
import org.chasen.mecab.Tagger;

/**
 * Tokenizes the input by the MeCab tokenizer.
 */
public class MeCabTokenizer extends Tokenizer
{
    public enum TermType
    {
        SURFACE,
        FEATURE,
        FORMATTED
    }

    public enum TypeAs
    {
        NONE,
        POSID,
        SURFACE,
        FEATURE,
        FORMATTED
    }

    public enum PayloadAs
    {
        NONE,
        POSID,
        SURFACE,
        FEATURE,
        FORMATTED
    }

    private String mecabArgs = null;
    private List<Integer> stopPosids = null;
    private TermType termType = TermType.SURFACE;
    private TermType termFallbackType = TermType.SURFACE;
    private TypeAs typeAs = TypeAs.NONE;
    private PayloadAs payloadAs = PayloadAs.NONE;
    private Tagger tagger = null;
    private Node node = null;
    private int pos = 0;
    private boolean started = false;

    private CharTermAttribute charTermAtt;
    private OffsetAttribute offsetAtt;
    private TypeAttribute typeAtt;
    private PayloadAttribute payloadAtt;

    public MeCabTokenizer(Reader input)
    {
        super(input);
        init(null);
    }

    public MeCabTokenizer(Reader input, String mecabArgs)
    {
        super(input);
        init(mecabArgs);
    }

    private void init(String mecabArgs)
    {
        setMecabArgs(mecabArgs);
        charTermAtt = (CharTermAttribute)addAttribute(CharTermAttribute.class);
        offsetAtt = (OffsetAttribute)addAttribute(OffsetAttribute.class);
        typeAtt = (TypeAttribute)addAttribute(TypeAttribute.class);
        payloadAtt = (PayloadAttribute)addAttribute(PayloadAttribute.class);
    }

    public void setMecabArgs(String mecabArgs)
    {
        this.mecabArgs = mecabArgs;
    }

    public String getMecabArgs()
    {
        return mecabArgs;
    }

    public void setStopPosids(List<Integer> stopPosids)
    {
        this.stopPosids = stopPosids;
    }

    public List<Integer> setStopPosids()
    {
        return stopPosids;
    }

    public void setTermType(TermType termType)
    {
        this.termType = termType;
    }

    public TermType getTermType()
    {
        return termType;
    }

    public void setTermFallbackType(TermType termType)
    {
        this.termFallbackType = termType;
    }

    public TermType getTermFallbackType()
    {
        return termFallbackType;
    }

    public void setTypeAs(TypeAs typeAs)
    {
        this.typeAs = typeAs;
    }

    public TypeAs getTypeAs()
    {
        return typeAs;
    }

    public void setPayloadAs(PayloadAs payloadAs)
    {
        this.payloadAs = payloadAs;
    }

    public PayloadAs getPayloadAs()
    {
        return payloadAs;
    }

    @Override
    public final boolean incrementToken() throws IOException
    {
        clearAttributes();

        if (!started)
        {
            started = true;
            pos = 0;
            if (tagger == null)
            {
                if (mecabArgs == null)
                {
                    tagger = new Tagger();
                }
                else
                {
                    tagger = new Tagger(mecabArgs);
                }
            }
            BufferedReader br = new BufferedReader(input);
            StringBuffer sb = new StringBuffer();
            int c;
            while ((c = br.read()) != -1)
            {
                sb.append((char)c);
            }
            String inputString = sb.toString();
            tagger.parse(inputString); // this will be required.
            node = tagger.parseToNode(inputString);
        }
        else
        {
            if (node != null)
            {
                node = node.getNext();
            }
        }

        for (;;)
        {
            while (node != null && (node.getStat() == MeCabConstants.MECAB_BOS_NODE || node.getStat() == MeCabConstants.MECAB_EOS_NODE))
            {
                node = node.getNext();
            }

            if (node == null)
            {
                return false;
            }

            if (stopPosids == null)
            {
                break;
            }
            int posid = node.getPosid();
            if (!stopPosids.contains(posid))
            {
                break;
            }
            node = node.getNext();
        }

        int len = node.getSurface().length();
        int gap = node.getRlength() - node.getLength(); // adjust length gap for non surface characters.
        int oldPos = pos + gap;
        pos += gap + len;

        if (TermType.FEATURE.equals(termType))
        {
            charTermAtt.setEmpty().append(node.getFeature());
        }
        else if (TermType.FORMATTED.equals(termType))
        {
            String formatted = tagger.formatNode(node);
            if (formatted == null || formatted.length() == 0)
            {
                if (TermType.FEATURE.equals(termFallbackType))
                {
                    formatted = node.getFeature();
                }
                else
                {
                    formatted = node.getSurface();
                }
            }
            charTermAtt.setEmpty().append(formatted);
        }
        else
        {
            charTermAtt.setEmpty().append(node.getSurface());
        }

        offsetAtt.setOffset(correctOffset(oldPos), correctOffset(oldPos + len));

        if (TypeAs.POSID.equals(typeAs))
        {
            String posId = new Integer(node.getPosid()).toString();
            typeAtt.setType(posId);
        }
        else if (TypeAs.SURFACE.equals(typeAs))
        {
            typeAtt.setType(node.getSurface());
        }
        else if (TypeAs.FEATURE.equals(typeAs))
        {
            typeAtt.setType(node.getFeature());
        }
        else if (TypeAs.FORMATTED.equals(typeAs))
        {
            typeAtt.setType(tagger.formatNode(node));
        }

        if (PayloadAs.POSID.equals(payloadAs))
        {
            byte[] posIdBytes = new byte[1];
            posIdBytes[0] = (byte)node.getPosid();
            payloadAtt.setPayload(new BytesRef(posIdBytes));
        }
        else if (PayloadAs.SURFACE.equals(payloadAs))
        {
            payloadAtt.setPayload(new BytesRef(node.getSurface().getBytes()));
        }
        else if (PayloadAs.FEATURE.equals(payloadAs))
        {
            payloadAtt.setPayload(new BytesRef(node.getFeature().getBytes()));
        }
        else if (PayloadAs.FORMATTED.equals(payloadAs))
        {
            payloadAtt.setPayload(new BytesRef(tagger.formatNode(node).getBytes()));
        }

        return true;
    }

    @Override
    public final void end()
    {
        final int finalOffset = correctOffset(pos);
        this.offsetAtt.setOffset(finalOffset, finalOffset);
        if (started)
        {
            started = false;
        }
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        if (started)
        {
            tagger.delete();
            tagger = null;
            started = false;
        }
    }

    @Override
    public void reset() throws IOException
    {
        super.reset();
        started = false;
    }
}
