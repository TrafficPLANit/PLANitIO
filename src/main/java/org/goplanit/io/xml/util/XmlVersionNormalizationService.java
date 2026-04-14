package org.goplanit.io.xml.util;

public class XmlVersionNormalizationService {

    // todo: when working in XML expand on this
    public PLANit normalize(Object parsedXml) {
        if (parsedXml instanceof org.goplanit.xml.generated.v2.PLANit) {
            return (PLANit) parsedXml;
        }

        if (parsedXml instanceof org.goplanit.xml.generated.v1.PLANit) {
            return VersionNormalizationMapper.INSTANCE.toV2((org.goplanit.xml.generated.v1.PLANit) parsedXml);
        }

        throw new IllegalArgumentException("Unknown XML version: " + parsedXml.getClass().getName());
    }
}
