package cz.tacr.elza.domain.bridge;

import org.apache.lucene.analysis.classic.ClassicTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.miscellaneous.LengthFilterFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class LuceneAnalyzerConfigurer implements LuceneAnalysisConfigurer {

	public final static String CLASSIC_TOKENIZER_CZ = "classic_tokenizer_cz"; 
	public final static String KEYWORD_TOKENIZER_CZ = "keyword_tokenizer_cz"; 

	@Override
    public void configure(LuceneAnalysisConfigurationContext context) {

		// https://stackoverflow.com/questions/58542870/getting-a-maxbyteslengthexceededexception-for-a-textfield
    	// Tokenizers: https://solr.apache.org/guide/8_2/tokenizers.html

		context.analyzer(CLASSIC_TOKENIZER_CZ)
            .custom()
            .tokenizer(ClassicTokenizerFactory.class)
            .tokenFilter(LowerCaseFilterFactory.class)
            .tokenFilter(ASCIIFoldingFilterFactory.class);
		// používáme keyword_tokenizer, kde řetězec == token, abychom získali přesnou shodu
        context.analyzer(KEYWORD_TOKENIZER_CZ)
            .custom()
            .tokenizer(KeywordTokenizerFactory.class)
            .tokenFilter(LengthFilterFactory.class)
            .param(LengthFilterFactory.MIN_KEY, "0")
            .param(LengthFilterFactory.MAX_KEY, "32766");
    }
}
