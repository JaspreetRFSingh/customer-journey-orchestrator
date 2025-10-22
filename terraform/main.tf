# Terraform configuration for AWS deployment
# Deploys EKS cluster, ECR, and supporting infrastructure

terraform {
  required_version = ">= 1.5.0"
  
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.23"
    }
    helm = {
      source  = "hashicorp/helm"
      version = "~> 2.11"
    }
  }
  
  backend "s3" {
    bucket = "journey-orchestrator-terraform-state"
    key    = "eks/terraform.tfstate"
    region = "us-east-1"
  }
}

provider "aws" {
  region = var.aws_region
  
  default_tags {
    tags = {
      Project     = "journey-orchestrator"
      Environment = var.environment
      ManagedBy   = "terraform"
    }
  }
}

# Variables
variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "cluster_name" {
  description = "EKS cluster name"
  type        = string
  default     = "journey-orchestrator"
}

variable "node_groups" {
  description = "EKS node group configuration"
  type = object({
    desired_size = number
    max_size     = number
    min_size     = number
    instance_types = list(string)
  })
  default = {
    desired_size   = 2
    max_size       = 5
    min_size       = 1
    instance_types = ["t3.medium"]
  }
}

# VPC for EKS
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"
  
  name = "${var.cluster_name}-vpc"
  cidr = "10.0.0.0/16"
  
  azs             = ["${var.aws_region}a", "${var.aws_region}b", "${var.aws_region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]
  
  enable_nat_gateway   = true
  single_nat_gateway   = true
  enable_dns_hostnames = true
  enable_dns_support   = true
  
  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }
  
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
  }
}

# EKS Cluster
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"
  
  cluster_name    = var.cluster_name
  cluster_version = "1.28"
  
  vpc_id     = module.vpc.vpc_id
  subnet_ids = module.vpc.private_subnets
  
  eks_managed_node_groups = {
    default = {
      desired_size = var.node_groups.desired_size
      max_size     = var.node_groups.max_size
      min_size     = var.node_groups.min_size
      
      instance_types = var.node_groups.instance_types
      capacity_type  = "ON_DEMAND"
      
      tags = {
        Environment = var.environment
      }
    }
  }
  
  cluster_endpoint_public_access = true
  
  tags = {
    Environment = var.environment
  }
}

# ECR Repository
resource "aws_ecr_repository" "journey_orchestrator" {
  name                 = "journey-orchestrator"
  image_tag_mutability = "MUTABLE"
  
  image_scanning_configuration {
    scan_on_push = true
  }
  
  encryption_configuration {
    encryption_type = "AES256"
  }
}

# IAM role for EKS service account (for accessing AWS services)
resource "aws_iam_role" "eks_service_role" {
  name = "${var.cluster_name}-service-role"
  
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRoleWithWebIdentity"
        Effect = "Allow"
        Principal = {
          Federated = module.eks.oidc_provider_arn
        }
        Condition = {
          StringEquals = {
            "${module.eks.oidc_provider}:sub" = "system:serviceaccount:journey-orchestrator:journey-orchestrator"
          }
        }
      }
    ]
  })
}

# Policy for accessing CloudWatch Logs
resource "aws_iam_role_policy" "cloudwatch_logs" {
  name = "${var.cluster_name}-cloudwatch-logs"
  role = aws_iam_role.eks_service_role.id
  
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# Outputs
output "cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.eks.cluster_endpoint
}

output "cluster_certificate_authority_data" {
  description = "EKS cluster CA data"
  value       = module.eks.cluster_certificate_authority_data
}

output "ecr_repository_url" {
  description = "ECR repository URL"
  value       = aws_ecr_repository.journey_orchestrator.repository_url
}

output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}
