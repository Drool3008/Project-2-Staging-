package org.apache.roller.weblogger.pojos;

import org.apache.lucene.document.Document;

/**
 * Interface for entities that can be indexed by Lucene.
 */
public interface Indexable {

    /**
     * Get the Lucene Document representing this entity.
     * 
     * @return The Lucene Document.
     */
    Document getDocument();

    /**
     * Get the unique ID of the entity.
     * 
     * @return The entity ID.
     */
    String getId();
}
