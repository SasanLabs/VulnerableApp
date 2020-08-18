package org.sasanlabs.service.vulnerability.xss.persistent;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/** @author preetkaran20@gmail.com KSASAN */
@Entity(name = "POSTS")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String content;
    /**
     * This is added such that one levels XSS payload doesn't interfere in other levels XSS payload.
     */
    private String levelIdentifier;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getLevelIdentifier() {
        return levelIdentifier;
    }

    public void setLevelIdentifier(String levelIdentifier) {
        this.levelIdentifier = levelIdentifier;
    }
}
