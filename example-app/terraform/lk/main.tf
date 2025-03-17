terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.89.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.7.1"
    }
    archive = {
      source  = "hashicorp/archive"
      version = "~> 2.7.0"
    }
  }
  required_version = "~> 1.0"
}
terraform {
  backend "s3" {
    bucket = "lightningkite-terraform"
    key    = "kiteui"
    region = "us-west-2"
  }
}

provider "aws" {
  region = "us-west-2"
}
provider "aws" {
  alias  = "acm"
  region = "us-east-1"
}

module "web" {
  source = "git@github.com:lightningkite/terraform-static-site.git"
  providers = {
    aws = aws
    aws.acm = aws.acm
  }
  deployment_name = "kiteui"
  dist_folder = "../../build/vite/dist"
  domain_name = "kiteui.cs.lightningkite.com"
  domain_name_zone = "cs.lightningkite.com"
  react_mode = true
}

