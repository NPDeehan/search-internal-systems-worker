package com.example.camunda.service;

import com.example.camunda.model.ExternalCompany;
import com.example.camunda.repository.ExternalCompanyRepository;
import com.example.camunda.exception.CompanyNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private ExternalCompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    private ExternalCompany testCompany;

    @BeforeEach
    void setUp() {
        testCompany = new ExternalCompany();
        testCompany.setCompanyId(1L);
        testCompany.setCompanyName("Test Company");
        testCompany.setAddress("123 Main St");
        testCompany.setContactPerson("John Doe");
        testCompany.setPhoneNumber("555-1234");
        testCompany.setCreatedAt(LocalDateTime.now());
        testCompany.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllCompanies_ShouldReturnAllCompanies() {
        // Arrange
        List<ExternalCompany> companies = List.of(testCompany);
        when(companyRepository.findAll()).thenReturn(companies);

        // Act
        List<ExternalCompany> result = companyService.getAllCompanies();

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).isEqualTo("Test Company");
        verify(companyRepository).findAll();
    }

    @Test
    void getCompanyById_WithValidId_ShouldReturnCompany() {
        // Arrange
        when(companyRepository.findById(1L)).thenReturn(Optional.of(testCompany));

        // Act
        ExternalCompany result = companyService.getCompanyById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
        assertThat(result.getAddress()).isEqualTo("123 Main St");
        verify(companyRepository).findById(1L);
    }

    @Test
    void getCompanyById_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(companyRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> companyService.getCompanyById(999L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("Company not found with ID: 999");
        
        verify(companyRepository).findById(999L);
    }

    @Test
    void saveCompany_WithValidCompany_ShouldSaveAndReturn() {
        // Arrange
        when(companyRepository.save(testCompany)).thenReturn(testCompany);

        // Act
        ExternalCompany result = companyService.saveCompany(testCompany);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getCompanyName()).isEqualTo("Test Company");
        assertThat(result.getContactPerson()).isEqualTo("John Doe");
        verify(companyRepository).save(testCompany);
    }

    @Test
    void deleteCompany_WithValidId_ShouldDeleteCompany() {
        // Arrange
        when(companyRepository.existsById(1L)).thenReturn(true);

        // Act
        companyService.deleteCompany(1L);

        // Assert
        verify(companyRepository).existsById(1L);
        verify(companyRepository).deleteById(1L);
    }

    @Test
    void deleteCompany_WithInvalidId_ShouldThrowException() {
        // Arrange
        when(companyRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> companyService.deleteCompany(999L))
                .isInstanceOf(CompanyNotFoundException.class)
                .hasMessageContaining("Company not found with ID: 999");
        
        verify(companyRepository).existsById(999L);
        verify(companyRepository, never()).deleteById(any());
    }

    @Test
    void getCompaniesByName_ShouldReturnFilteredCompanies() {
        // Arrange
        List<ExternalCompany> companies = List.of(testCompany);
        when(companyRepository.findByCompanyNameContainingIgnoreCase("Test")).thenReturn(companies);

        // Act
        List<ExternalCompany> result = companyService.getCompaniesByName("Test");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompanyName()).contains("Test");
        verify(companyRepository).findByCompanyNameContainingIgnoreCase("Test");
    }
}
