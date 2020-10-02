package org.sasanlabs.service.vulnerability.xxe.dao;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import org.sasanlabs.service.vulnerability.xxe.bean.Book;

/** @author preetkaran20@gmail.com KSASAN */
@Entity(name = "BOOKS")
public class BookEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    /**
     * This is added such that one level's books information doesn't interfere to other level's
     * books. For now XXE doesn't require this but going further it might be needed.
     */
    private String levelIdentifier;

    private String name;

    private String author;

    private String isbn;

    private String publisher;

    private String others;

    public BookEntity() {}

    public BookEntity(Book bookBean, String levelIdentifier) {
        this.setAuthor(bookBean.getAuthor());
        this.setIsbn(bookBean.getIsbn());
        this.setPublisher(bookBean.getPublisher());
        this.setName(bookBean.getName());
        this.setOthers(bookBean.getOthers());
        this.setLevelIdentifier(levelIdentifier);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLevelIdentifier() {
        return levelIdentifier;
    }

    public void setLevelIdentifier(String levelIdentifier) {
        this.levelIdentifier = levelIdentifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getOthers() {
        return others;
    }

    public void setOthers(String others) {
        this.others = others;
    }
}
