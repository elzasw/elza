package cz.tacr.elza.service.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 14.11.2017
 */
@Configuration
@ConfigurationProperties("elza.attachment")
public class AttachmentConfig {
    private List<MimeDef> mimeDefs = new ArrayList<>();

    public List<MimeDef> getMimeDefs() {
        return mimeDefs;
    }

    public static class Generator {
        private String outputMimeType;
        private String command;
        private String outputFileName;

        public String getOutputMimeType() {
            return outputMimeType;
        }

        public String getCommand() {
            return command;
        }

        public void setOutputMimeType(String outputMimeType) {
            this.outputMimeType = outputMimeType;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getOutputFileName() {
            return outputFileName;
        }

        public void setOutputFileName(String outputFileName) {
            this.outputFileName = outputFileName;
        }
    }

    public static class MimeDef {
        private String mimeType;
        private Boolean editable;
        private List<Generator> generators = new ArrayList<>();

        public String getMimeType() {
            return mimeType;
        }

        public Boolean getEditable() {
            return editable;
        }

        public List<Generator> getGenerators() {
            return generators;
        }

        public Generator findGenerator(final String outputMimeType) {
            for (Generator generator : getGenerators()) {
                if (generator.getOutputMimeType().equalsIgnoreCase(outputMimeType)) {
                    return generator;
                }
            }
            return null;
        }

        public void setMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        public void setEditable(Boolean editable) {
            this.editable = editable;
        }

        public void setGenerators(List<Generator> generators) {
            this.generators = generators;
        }
    }


    public MimeDef findMimeDef(final String mimeType) {
        for (MimeDef mimeDef : mimeDefs) {
            if (mimeDef.getMimeType().equalsIgnoreCase(mimeType)) {
                return mimeDef;
            }
        }
        return null;
    }
}
