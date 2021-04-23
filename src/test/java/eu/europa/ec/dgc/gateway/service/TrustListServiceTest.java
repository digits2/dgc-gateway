package eu.europa.ec.dgc.gateway.service;

import eu.europa.ec.dgc.gateway.entity.SignerInformationEntity;
import eu.europa.ec.dgc.gateway.entity.TrustedPartyEntity;
import eu.europa.ec.dgc.gateway.model.TrustList;
import eu.europa.ec.dgc.gateway.model.TrustListType;
import eu.europa.ec.dgc.gateway.repository.SignerInformationRepository;
import eu.europa.ec.dgc.gateway.repository.TrustedPartyRepository;
import eu.europa.ec.dgc.gateway.testdata.CertificateTestUtils;
import eu.europa.ec.dgc.gateway.testdata.TrustedPartyTestHelper;
import eu.europa.ec.dgc.utils.CertificateUtils;
import java.security.KeyPairGenerator;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TrustListServiceTest {

    @Autowired
    SignerInformationRepository signerInformationRepository;

    @Autowired
    TrustedPartyRepository trustedPartyRepository;

    @Autowired
    TrustedPartyTestHelper trustedPartyTestHelper;

    @Autowired
    TrustListService trustListService;

    @Autowired
    CertificateUtils certificateUtils;

    X509Certificate certUploadDe, certUploadEu, certCscaDe, certCscaEu, certAuthDe, certAuthEu, certDscDe, certDscEu;

    @Before
    public void testData() throws Exception {
        trustedPartyRepository.deleteAll();
        signerInformationRepository.deleteAll();

        certUploadDe = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.UPLOAD, "DE");
        certUploadEu = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.UPLOAD, "EU");
        certCscaDe = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.CSCA, "DE");
        certCscaEu = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.CSCA, "EU");
        certAuthDe = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.AUTHENTICATION, "DE");
        certAuthEu = trustedPartyTestHelper.getCert(TrustedPartyEntity.CertificateType.AUTHENTICATION, "EU");

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ec");
        certDscDe = CertificateTestUtils.generateCertificate(keyPairGenerator.generateKeyPair(), "DE", "Test");
        certDscEu = CertificateTestUtils.generateCertificate(keyPairGenerator.generateKeyPair(), "EU", "Test");

        signerInformationRepository.save(new SignerInformationEntity(
            null,
            ZonedDateTime.now(),
            "DE",
            certificateUtils.getCertThumbprint(certDscDe),
            Base64.getEncoder().encodeToString(certDscDe.getEncoded()),
            "sig1",
            SignerInformationEntity.CertificateType.DSC
        ));

        signerInformationRepository.save(new SignerInformationEntity(
            null,
            ZonedDateTime.now(),
            "EU",
            certificateUtils.getCertThumbprint(certDscEu),
            Base64.getEncoder().encodeToString(certDscEu.getEncoded()),
            "sig2",
            SignerInformationEntity.CertificateType.DSC
        ));
    }

    @Test
    public void testTrustListWithoutFilter() throws Exception {
        List<TrustList> trustList = trustListService.getTrustList();

        Assert.assertEquals(8, trustList.size());

        assertTrustListItem(trustList, certDscDe, "DE", TrustListType.DSC, "sig1");
        assertTrustListItem(trustList, certDscEu, "EU", TrustListType.DSC, "sig2");
        assertTrustListItem(trustList, certCscaDe, "DE", TrustListType.CSCA, null);
        assertTrustListItem(trustList, certCscaEu, "EU", TrustListType.CSCA, null);
        assertTrustListItem(trustList, certUploadDe, "DE", TrustListType.UPLOAD, null);
        assertTrustListItem(trustList, certUploadEu, "EU", TrustListType.UPLOAD, null);
        assertTrustListItem(trustList, certAuthDe, "DE", TrustListType.AUTHENTICATION, null);
        assertTrustListItem(trustList, certAuthEu, "EU", TrustListType.AUTHENTICATION, null);
    }

    @Test
    public void testTrustListFilterByType() throws Exception {
        List<TrustList> trustList = trustListService.getTrustList(TrustListType.DSC);
        Assert.assertEquals(2, trustList.size());
        assertTrustListItem(trustList, certDscDe, "DE", TrustListType.DSC, "sig1");
        assertTrustListItem(trustList, certDscEu, "EU", TrustListType.DSC, "sig2");

        trustList = trustListService.getTrustList(TrustListType.CSCA);
        Assert.assertEquals(2, trustList.size());
        assertTrustListItem(trustList, certCscaDe, "DE", TrustListType.CSCA, null);
        assertTrustListItem(trustList, certCscaEu, "EU", TrustListType.CSCA, null);

        trustList = trustListService.getTrustList(TrustListType.UPLOAD);
        Assert.assertEquals(2, trustList.size());
        assertTrustListItem(trustList, certUploadDe, "DE", TrustListType.UPLOAD, null);
        assertTrustListItem(trustList, certUploadEu, "EU", TrustListType.UPLOAD, null);

        trustList = trustListService.getTrustList(TrustListType.AUTHENTICATION);
        Assert.assertEquals(2, trustList.size());
        assertTrustListItem(trustList, certAuthDe, "DE", TrustListType.AUTHENTICATION, null);
        assertTrustListItem(trustList, certAuthEu, "EU", TrustListType.AUTHENTICATION, null);
    }

    @Test
    public void testTrustListFilterByTypeAndCountry() throws Exception {
        List<TrustList> trustList = trustListService.getTrustList(TrustListType.DSC, "DE");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certDscDe, "DE", TrustListType.DSC, "sig1");
        trustList = trustListService.getTrustList(TrustListType.DSC, "EU");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certDscEu, "EU", TrustListType.DSC, "sig2");

        trustList = trustListService.getTrustList(TrustListType.CSCA, "DE");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certCscaDe, "DE", TrustListType.CSCA, null);
        trustList = trustListService.getTrustList(TrustListType.CSCA, "EU");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certCscaEu, "EU", TrustListType.CSCA, null);

        trustList = trustListService.getTrustList(TrustListType.UPLOAD, "DE");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certUploadDe, "DE", TrustListType.UPLOAD, null);
        trustList = trustListService.getTrustList(TrustListType.UPLOAD, "EU");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certUploadEu, "EU", TrustListType.UPLOAD, null);

        trustList = trustListService.getTrustList(TrustListType.AUTHENTICATION, "DE");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certAuthDe, "DE", TrustListType.AUTHENTICATION, null);
        trustList = trustListService.getTrustList(TrustListType.AUTHENTICATION, "EU");
        Assert.assertEquals(1, trustList.size());
        assertTrustListItem(trustList, certAuthEu, "EU", TrustListType.AUTHENTICATION, null);
    }

    private void assertTrustListItem(List<TrustList> trustList, X509Certificate certificate, String country, TrustListType trustListType, String signature) throws CertificateEncodingException {
        Optional<TrustList> trustListOptional = trustList
            .stream()
            .filter(tl -> tl.getKid().equals(certificateUtils.getCertKid(certificate)))
            .findFirst();

        Assert.assertTrue(trustListOptional.isPresent());

        TrustList trustListItem = trustListOptional.get();

        Assert.assertEquals(certificateUtils.getCertKid(certificate), trustListItem.getKid());
        Assert.assertEquals(country, trustListItem.getCountry());
        Assert.assertEquals(trustListType, trustListItem.getCertificateType());
        Assert.assertEquals(certificateUtils.getCertThumbprint(certificate), trustListItem.getThumbprint());
        Assert.assertEquals(Base64.getEncoder().encodeToString(certificate.getEncoded()), trustListItem.getRawData());

        if (signature != null) {
            Assert.assertEquals(signature, trustListItem.getSignature());
        }
    }

}
