package com.book.repository;

import com.book.BookApiApp;
import com.book.QuickPerfPerBeanConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.quickperf.junit5.QuickPerfTest;
import org.quickperf.sql.annotation.ExpectSelect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.*;

@Import(QuickPerfPerBeanConfig.class)
@SpringBootTest(classes = BookApiApp.class)
//@DataJpaTest
@QuickPerfTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

	@Test
    @ExpectSelect(1)
	void findAllWithEagerRelationships() {
        bookRepository.findAllWithEagerRelationships();
	}

	@Test
    @ExpectSelect(1)
	void findOneWithEagerRelationships() {
	    bookRepository.findOneWithEagerRelationships(1L);
	}
}
