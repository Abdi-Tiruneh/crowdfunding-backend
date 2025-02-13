package com.dxvalley.crowdfunding.service.impl;

import com.dxvalley.crowdfunding.dto.CampaignAddRequestDto;
import com.dxvalley.crowdfunding.dto.CampaignDTO;
import com.dxvalley.crowdfunding.dto.CampaignLikeDTO;
import com.dxvalley.crowdfunding.dto.mapper.CampaignDTOMapper;
import com.dxvalley.crowdfunding.exception.ResourceAlreadyExistsException;
import com.dxvalley.crowdfunding.exception.ResourceNotFoundException;
import com.dxvalley.crowdfunding.exception.UserNotEnabledException;
import com.dxvalley.crowdfunding.model.*;
import com.dxvalley.crowdfunding.repository.*;
import com.dxvalley.crowdfunding.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CampaignServiceImpl implements CampaignService {

    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private CampaignBankAccountRepository campaignBankAccountRepository;
    @Autowired
    private CollaboratorRepository collaboratorRepository;
    @Autowired
    private RewardRepository rewardRepository;
    @Autowired
    private PromotionRepository promotionRepository;
    @Autowired
    private FundingTypeService fundingTypeService;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    CampaignSubCategoryService campaignSubCategoryService;
    @Autowired
    private UserService userService;
    @Autowired
    private CampaignDTOMapper campaignDTOMapper;
    @Autowired
    private CampaignLikeRepository campaignLikeRepository;
    @Autowired
    private FileUploadService fileUploadService;
    @Autowired
    private DateTimeFormatter dateTimeFormatter;
    private final Logger logger = LoggerFactory.getLogger(CampaignServiceImpl.class);


    @Override
    public Campaign addCampaign(CampaignAddRequestDto campaignAddRequestDto) {
        try {

            FundingType fundingType = fundingTypeService.getFundingTypeById(campaignAddRequestDto.getFundingTypeId());
            CampaignSubCategory campaignSubCategory = campaignSubCategoryService.getCampaignSubCategoryById(campaignAddRequestDto.getCampaignSubCategoryId());
            var user = userService.getUserByUsername(campaignAddRequestDto.getOwner());
            if (!user.getIsEnabled()) throw new UserNotEnabledException("User is not enabled to add campaigns");

            Campaign campaign = new Campaign();
            campaign.setTitle(campaignAddRequestDto.getTitle());
            campaign.setCity(campaignAddRequestDto.getCity());
            campaign.setOwner(user.getUsername());
            campaign.setCampaignSubCategory(campaignSubCategory);
            campaign.setFundingType(fundingType);
            campaign.setCreatedAt(LocalDateTime.now().format(dateTimeFormatter));
            campaign.setEditedAt(LocalDateTime.now().format(dateTimeFormatter));
            campaign.setIsEnabled(false);
            campaign.setCampaignStage(CampaignStage.INITIAL);
            campaign.setGoalAmount((double) 0);
            campaign.setTotalAmountCollected((double) 0);
            campaign.setNumberOfBackers(0);
            campaign.setNumberOfLikes(0);
            campaign.setCampaignDuration((short) 0);

            logger.info("New campaign Added");
            return campaignRepository.save(campaign);
        } catch (DataAccessException ex) {
            logger.error("Error Adding campaigns: {}", ex.getMessage());
            throw new RuntimeException("Error Adding campaigns", ex);
        }
    }

    @Override
    public CampaignDTO editCampaign(Long campaignId, CampaignDTO campaignDTO) {
        try {
            Campaign campaign = utilGetCampaignById(campaignId);

            campaign.setTitle(Optional.ofNullable(campaignDTO.getTitle()).orElse(campaign.getTitle()));
            campaign.setShortDescription(Optional.ofNullable(campaignDTO.getShortDescription()).orElse(campaign.getShortDescription()));
            campaign.setCity(Optional.ofNullable(campaignDTO.getCity()).orElse(campaign.getCity()));
            campaign.setProjectType(Optional.ofNullable(campaignDTO.getProjectType()).orElse(campaign.getProjectType()));
            campaign.setGoalAmount(Optional.ofNullable(campaignDTO.getGoalAmount()).orElse(campaign.getGoalAmount()));
            campaign.setCampaignDuration(Optional.ofNullable(campaignDTO.getCampaignDuration()).orElse(campaign.getCampaignDuration()));
            campaign.setRisks(Optional.ofNullable(campaignDTO.getRisks()).orElse(campaign.getRisks()));
            campaign.setDescription(Optional.ofNullable(campaignDTO.getDescription()).orElse(campaign.getDescription()));
            campaign.setCommissionRate(Optional.ofNullable(campaignDTO.getCommissionRate()).orElse(campaign.getCommissionRate()));
            campaign.setEditedAt(LocalDateTime.now().format(dateTimeFormatter));

            var result = campaignRepository.save(campaign);
            logger.info("Campaign edited successfully for campaign ID: {}", campaignId);
            return campaignDTOMapper.applyById(result);
        } catch (DataAccessException ex) {
            logger.error("Error editing campaign with ID: {}", campaignId, ex);
            throw new RuntimeException("Error editing campaign with ID", ex);
        }
    }

    @Override
    public CampaignDTO uploadMedias(Long campaignId, MultipartFile campaignImage, String campaignVideo) {
        try {
            Campaign campaign = utilGetCampaignById(campaignId);
            String imageUrl = null;
            if (campaignImage != null)
                imageUrl = fileUploadService.uploadFile(campaignImage);

            campaign.setImageUrl(Optional.ofNullable(imageUrl).orElse(campaign.getImageUrl()));
            campaign.setVideoLink(Optional.ofNullable(campaignVideo).orElse(campaign.getVideoLink()));
            campaign.setEditedAt(LocalDateTime.now().format(dateTimeFormatter));
            Campaign result = campaignRepository.save(campaign);
            logger.info("Campaign media updated successfully for campaign ID: {}", campaignId);
            return campaignDTOMapper.apply(result);
        } catch (DataAccessException ex) {
            logger.error("Error uploading campaign media for campaign ID: {}", campaignId, ex);
            throw new RuntimeException("Error uploading campaign media", ex);
        }
    }

    @Override
    public String likeCampaign(CampaignLikeDTO campaignLikeDTO) {
        try {
            var campaignLike = new CampaignLike();
            var result = campaignLikeRepository.findByCampaignCampaignIdAndUserUserId(campaignLikeDTO.getCampaignId(), campaignLikeDTO.getUserId());
            var campaign = utilGetCampaignById(campaignLikeDTO.getCampaignId());
            if (result != null) {
                campaignLikeRepository.delete(result);
                campaign.setNumberOfLikes(campaign.getNumberOfLikes() - 1);
                campaignRepository.save(campaign);
                logger.info("campaign with id {} Disliked", campaignLikeDTO.getCampaignId());
                return "Disliked Successfully";
            }
            campaignLike.setUser(userService.utilGetUserByUserId(campaignLikeDTO.getUserId()));
            campaignLike.setCampaign(campaign);
            campaignLikeRepository.save(campaignLike);
            campaign.setNumberOfLikes(campaign.getNumberOfLikes() + 1);
            campaignRepository.save(campaign);

            logger.info("campaign with id {} Liked", campaignLikeDTO.getCampaignId());
            return "Liked Successfully";
        } catch (DataAccessException ex) {
            logger.error("Error Liking campaigns: {}", ex.getMessage());
            throw new RuntimeException("Error Liking campaigns", ex);
        }
    }

    @Override
    public CampaignDTO pauseOrResumeCampaign(Long campaignID) {
        Campaign campaign = utilGetCampaignById(campaignID);
        if (campaign.getCampaignStage().equals(CampaignStage.PAUSED))
            campaign.setCampaignStage(CampaignStage.FUNDING);
        else
            campaign.setCampaignStage(CampaignStage.PAUSED);
        var result = campaignRepository.save(campaign);
        return campaignDTOMapper.apply(result);
    }

    @Override
    public List<CampaignDTO> getCampaigns() {
        try {
            List<Campaign> campaigns = campaignRepository.findCampaignsByCampaignStageIn(List.of(CampaignStage.FUNDING, CampaignStage.COMPLETED));
            if (campaigns.isEmpty())
                throw new ResourceNotFoundException("Currently, There is no campaign.");

            logger.info("Retrieved {} campaigns", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns", ex);
        }
    }

    @Override
    public Campaign enableCampaign(Long campaignId) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Campaign campaign = utilGetCampaignById(campaignId);
            if (campaign.getIsEnabled()) {
                throw new ResourceAlreadyExistsException("This campaign is already enabled.");
            }
            campaign.setIsEnabled(true);
            campaign.setCampaignStage(CampaignStage.FUNDING);
            campaign.setEnabledAt(now.format(dateTimeFormatter));
            campaign.setExpiredAt(now.plusDays(campaign.getCampaignDuration()).format(dateTimeFormatter));
            logger.info("Campaign with id {} enabled", campaignId);
            return campaignRepository.save(campaign);
        } catch (DataAccessException ex) {
            logger.error("Error retrieving Enabling campaigns : {}", ex.getMessage());
            throw new RuntimeException("Error retrieving Enabling campaigns ", ex);
        }
    }

    @Override
    public List<CampaignDTO> getEnabledCampaigns() {
        try {
            var campaigns = campaignRepository.findCampaignsByIsEnabled(true);
            if (campaigns.isEmpty()) {
                throw new ResourceNotFoundException("Currently, There is no Enabled campaign.");
            }
            logger.info("Retrieved {} enabled campaigns", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving Enabled campaigns: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving Enabled campaigns", ex);
        }
    }

    @Override
    public CampaignDTO getCampaignById(Long campaignId) {
        try {
            Campaign campaign = utilGetCampaignById(campaignId);

            CampaignDTO campaignDTO = campaignDTOMapper.applyById(campaign);
            var campaignBankAccount = campaignBankAccountRepository.findCampaignBankAccountByCampaignId(campaignId);
            var collaborators = collaboratorRepository.findAllCollaboratorByCampaignId(campaignId);
            var rewards = rewardRepository.findRewardsByCampaignId(campaignId);
            var promotions = promotionRepository.findPromotionByCampaignId(campaignId);
            var user = userService.getUserByUsername(campaign.getOwner());
            var contributors = paymentService.getPaymentByCampaignId(campaignId);

            if (campaignBankAccount.isPresent()) campaignDTO.setCampaignBankAccount(campaignBankAccount.get());
            campaignDTO.setCollaborators(collaborators);
            campaignDTO.setRewards(rewards);
            campaignDTO.setPromotions(promotions);
            campaignDTO.setContributors(contributors);
            campaignDTO.setOwnerFullName(user.getFullName());
            campaignDTO.setNumberOfBackers(contributors.size());
            logger.info("get Campaign with id {}", campaignId);
            return campaignDTO;
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by Id: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by Id", ex);
        }
    }

    @Override
    public List<CampaignDTO> getCampaignByCategory(Long categoryId) {
        try {
            var campaigns = campaignRepository.findCampaignsByCampaignSubCategoryCampaignCategoryCampaignCategoryIdAndCampaignStageIn(categoryId, List.of(CampaignStage.FUNDING, CampaignStage.COMPLETED));
            if (campaigns.isEmpty()) {
                throw new ResourceNotFoundException("There is no campaign for this category.");
            }
            logger.info("Retrieved {} campaigns by category", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by category: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by category ", ex);
        }
    }

    @Override
    public List<CampaignDTO> getCampaignBySubCategory(Long subCategoryId) {
        try {
            var campaigns = campaignRepository.findCampaignsByCampaignSubCategoryCampaignSubCategoryIdAndCampaignStageIn(subCategoryId, List.of(CampaignStage.FUNDING, CampaignStage.COMPLETED));
            if (campaigns.isEmpty()) {
                throw new ResourceNotFoundException("There is no campaign for this sub-category.");
            }
            logger.info("Retrieved {} campaigns by sub-category", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by sub-category: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by sub-category ", ex);
        }
    }

    @Override
    public List<CampaignDTO> getCampaignsByOwner(String owner) {
        try {
            List<Campaign> campaigns = campaignRepository.findCampaignsByOwner(owner);
            if (campaigns.isEmpty()) throw new ResourceNotFoundException("There is no campaign for this User.");
            logger.info("Retrieved {} campaigns by owner", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by owner: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by owner ", ex);
        }
    }

    @Override
    public List<CampaignDTO> searchCampaigns(String searchParam) {
        try {
            // Clean up the search parameter string by trimming whitespace and splitting by non-word characters
            String[] searchParamArray = searchParam.trim().split("\\W+");
            // Join the cleaned-up search parameters with "|" to create a regex search pattern
            String searchPattern = String.join("|", searchParamArray);
            var campaigns = campaignRepository.searchForCampaigns(searchPattern);

            if (campaigns.isEmpty()) {
                throw new ResourceNotFoundException("No campaigns found with this search parameter.");
            }
            logger.info("Retrieved {} campaigns by searchParam", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by searchParam: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by searchParam ", ex);
        }
    }

    @Override
    public List<CampaignDTO> getCampaignsByFundingType(Long fundingTypeId) {
        try {
            var campaigns = campaignRepository.findCampaignsByFundingTypeFundingTypeIdAndCampaignStageIn(fundingTypeId, List.of(CampaignStage.FUNDING, CampaignStage.COMPLETED));
            if (campaigns.isEmpty()) throw new ResourceNotFoundException("There is no campaign for this Funding Type.");
            logger.info("Retrieved {} campaigns Funding Type", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by Funding Type: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by Funding Type ", ex);
        }
    }

    @Override
    public List<CampaignDTO> getCampaignsByStage(String campaignStage) {
        try {
            CampaignStage result = CampaignStage.lookup(campaignStage);
            var campaigns = campaignRepository.findCampaignsByCampaignStage(result);
            if (campaigns.isEmpty())
                throw new ResourceNotFoundException("There is no campaign at " + campaignStage + " stage.");

            logger.info("Retrieved {} campaigns by stage", campaigns.size());
            return campaigns.stream().map(campaignDTOMapper).collect(Collectors.toList());
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by stage: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by stage ", ex);
        }
    }

    @Override
    public void deleteCampaign(Long campaignId) {
        try {
            var campaign = getCampaignById(campaignId);
            Optional<CampaignBankAccount> campaignBankAccount = Optional.ofNullable(campaign.getCampaignBankAccount());
            Optional<List<Reward>> rewards = Optional.ofNullable(campaign.getRewards());
            Optional<List<Collaborator>> collaborators = Optional.ofNullable(campaign.getCollaborators());
            Optional<List<Promotion>> promotions = Optional.ofNullable(campaign.getPromotions());
            Optional<List<Payment>> contributors = Optional.ofNullable(campaign.getContributors());

            campaignBankAccount.ifPresent(campaignBankAccountRepository::delete);
            rewards.ifPresent(rewardRepository::deleteAll);
            collaborators.ifPresent(collaboratorRepository::deleteAll);
            promotions.ifPresent(promotionRepository::deleteAll);
            contributors.ifPresent(paymentRepository::deleteAll);

            logger.info("Campaign Deleted");
            campaignRepository.deleteById(campaignId);
        } catch (DataAccessException ex) {
            logger.error("Error Deleting campaigns: {}", ex.getMessage());
            throw new RuntimeException("Error Deleting campaigns ", ex);
        }
    }

    //    utils methods for this class
    @Override
    public Campaign utilGetCampaignById(Long campaignId) {
        try {
            Campaign campaign = campaignRepository.findCampaignByCampaignId(campaignId)
                    .orElseThrow(() -> new ResourceNotFoundException("There is no campaign with this ID."));
            return campaign;
        } catch (DataAccessException ex) {
            logger.error("Error retrieving campaigns by Id: {}", ex.getMessage());
            throw new RuntimeException("Error retrieving campaigns by Id", ex);
        }

    }

}