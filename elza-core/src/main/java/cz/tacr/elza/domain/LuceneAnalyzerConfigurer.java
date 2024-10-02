package cz.tacr.elza.domain;

import org.apache.lucene.analysis.classic.ClassicTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class LuceneAnalyzerConfigurer implements LuceneAnalysisConfigurer {

	public final static String CLASSIC_TOKENIZER_CZ = "classic_cz"; 
	public final static String KEYWORD_TOKENIZER_CZ = "whitespace_cz"; 

	@Override
    public void configure(LuceneAnalysisConfigurationContext context) {

		// https://stackoverflow.com/questions/58542870/getting-a-maxbyteslengthexceededexception-for-a-textfield
    	// Tokenizers: https://solr.apache.org/guide/8_2/tokenizers.html

		context.analyzer(CLASSIC_TOKENIZER_CZ)
            .custom()
            .tokenizer(ClassicTokenizerFactory.class)
            .tokenFilter(LowerCaseFilterFactory.class)
            .tokenFilter(ASCIIFoldingFilterFactory.class);
        context.analyzer(KEYWORD_TOKENIZER_CZ)
            .custom()
            .tokenizer(KeywordTokenizerFactory.class);
    }
}
