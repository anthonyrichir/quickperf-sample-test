package com.book.web.rest;

import com.book.BookApiApp;
import com.book.domain.Editor;
import com.book.repository.EditorRepository;
import com.book.web.rest.errors.ExceptionTranslator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Validator;

import javax.persistence.EntityManager;
import java.util.List;

import static com.book.web.rest.TestUtil.createFormattingConversionService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the {@Link EditorResource} REST controller.
 */
@SpringBootTest(classes = BookApiApp.class)
public class EditorResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    @Autowired
    private EditorRepository editorRepository;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    @Autowired
    private Validator validator;

    private MockMvc restEditorMockMvc;

    private Editor editor;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final EditorResource editorResource = new EditorResource(editorRepository);
        this.restEditorMockMvc = MockMvcBuilders.standaloneSetup(editorResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter)
            .setValidator(validator).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Editor createEntity(EntityManager em) {
        Editor editor = new Editor()
            .name(DEFAULT_NAME);
        return editor;
    }
    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Editor createUpdatedEntity(EntityManager em) {
        Editor editor = new Editor()
            .name(UPDATED_NAME);
        return editor;
    }

    @BeforeEach
    public void initTest() {
        editor = createEntity(em);
    }

    @Test
    @Transactional
    public void createEditor() throws Exception {
        int databaseSizeBeforeCreate = editorRepository.findAll().size();

        // Create the Editor
        restEditorMockMvc.perform(post("/api/editors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(editor)))
            .andExpect(status().isCreated());

        // Validate the Editor in the database
        List<Editor> editorList = editorRepository.findAll();
        assertThat(editorList).hasSize(databaseSizeBeforeCreate + 1);
        Editor testEditor = editorList.get(editorList.size() - 1);
        assertThat(testEditor.getName()).isEqualTo(DEFAULT_NAME);
    }

    @Test
    @Transactional
    public void createEditorWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = editorRepository.findAll().size();

        // Create the Editor with an existing ID
        editor.setId(1L);

        // An entity with an existing ID cannot be created, so this API call must fail
        restEditorMockMvc.perform(post("/api/editors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(editor)))
            .andExpect(status().isBadRequest());

        // Validate the Editor in the database
        List<Editor> editorList = editorRepository.findAll();
        assertThat(editorList).hasSize(databaseSizeBeforeCreate);
    }


    @Test
    @Transactional
    public void getAllEditors() throws Exception {
        // Initialize the database
        editorRepository.saveAndFlush(editor);

        // Get all the editorList
        restEditorMockMvc.perform(get("/api/editors?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(editor.getId().intValue())))
            .andExpect(jsonPath("$.[*].name").value(hasItem(DEFAULT_NAME.toString())));
    }
    
    @Test
    @Transactional
    public void getEditor() throws Exception {
        // Initialize the database
        editorRepository.saveAndFlush(editor);

        // Get the editor
        restEditorMockMvc.perform(get("/api/editors/{id}", editor.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(editor.getId().intValue()))
            .andExpect(jsonPath("$.name").value(DEFAULT_NAME.toString()));
    }

    @Test
    @Transactional
    public void getNonExistingEditor() throws Exception {
        // Get the editor
        restEditorMockMvc.perform(get("/api/editors/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateEditor() throws Exception {
        // Initialize the database
        editorRepository.saveAndFlush(editor);

        int databaseSizeBeforeUpdate = editorRepository.findAll().size();

        // Update the editor
        Editor updatedEditor = editorRepository.findById(editor.getId()).get();
        // Disconnect from session so that the updates on updatedEditor are not directly saved in db
        em.detach(updatedEditor);
        updatedEditor
            .name(UPDATED_NAME);

        restEditorMockMvc.perform(put("/api/editors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(updatedEditor)))
            .andExpect(status().isOk());

        // Validate the Editor in the database
        List<Editor> editorList = editorRepository.findAll();
        assertThat(editorList).hasSize(databaseSizeBeforeUpdate);
        Editor testEditor = editorList.get(editorList.size() - 1);
        assertThat(testEditor.getName()).isEqualTo(UPDATED_NAME);
    }

    @Test
    @Transactional
    public void updateNonExistingEditor() throws Exception {
        int databaseSizeBeforeUpdate = editorRepository.findAll().size();

        // Create the Editor

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        restEditorMockMvc.perform(put("/api/editors")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(editor)))
            .andExpect(status().isBadRequest());

        // Validate the Editor in the database
        List<Editor> editorList = editorRepository.findAll();
        assertThat(editorList).hasSize(databaseSizeBeforeUpdate);
    }

    @Test
    @Transactional
    public void deleteEditor() throws Exception {
        // Initialize the database
        editorRepository.saveAndFlush(editor);

        int databaseSizeBeforeDelete = editorRepository.findAll().size();

        // Delete the editor
        restEditorMockMvc.perform(delete("/api/editors/{id}", editor.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isNoContent());

        // Validate the database is empty
        List<Editor> editorList = editorRepository.findAll();
        assertThat(editorList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Editor.class);
        Editor editor1 = new Editor();
        editor1.setId(1L);
        Editor editor2 = new Editor();
        editor2.setId(editor1.getId());
        assertThat(editor1).isEqualTo(editor2);
        editor2.setId(2L);
        assertThat(editor1).isNotEqualTo(editor2);
        editor1.setId(null);
        assertThat(editor1).isNotEqualTo(editor2);
    }
}
