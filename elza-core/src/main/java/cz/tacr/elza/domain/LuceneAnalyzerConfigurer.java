package cz.tacr.elza.domain;

import org.apache.lucene.analysis.classic.ClassicTokenizerFactory;
import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;

public class LuceneAnalyzerConfigurer implements LuceneAnalysisConfigurer {

	@Override
    public void configure(LuceneAnalysisConfigurationContext context) {
        context.analyzer("cz").custom()
    		// https://stackoverflow.com/questions/58542870/getting-a-maxbyteslengthexceededexception-for-a-textfield
        	// https://solr.apache.org/guide/solr/latest/indexing-guide/tokenizers.html
        	.tokenizer(ClassicTokenizerFactory.class)
        	.tokenFilter(LowerCaseFilterFactory.class)
        	.tokenFilter(ASCIIFoldingFilterFactory.class);
    }
}
