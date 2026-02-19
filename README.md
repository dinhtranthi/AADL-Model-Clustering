# AADL-Model-Clustering

This repository contains the folders and files that support the **structural-semantic clustering approach**, which automatically clusters sets of architectures by considering both **structural** and **semantic** information.  
We built a dataset of **1,202 models** collected from GitHub and applied the proposed clustering approach to discover groups of related models.

---

## Repository Structure

### `aadl_similarity_calculation/`
This folder includes the project files for converting models into **graph structures** and computing **similarity indices**.

### `aadl_clustering/`
This folder contains the **clustering project files**.  
The **ground truth** for both *application domain* and *specific goal* can be found in: aadl_clustering/validate/

### `aadl_models-main.zip`
This zipped folder contains the **original models** used to create the **ground truth**.

### `aadl_top_updated_repos.py`
This script queries the GitHub Search API to identify **AADL-related repositories** that have been actively updated over time, and exports the top updated repositories per month as CSV files.

---

## Summary
- Dataset: 1,202 AADL models collected from GitHub  
- Purpose: Structural-semantic clustering of software architectures  
- Output: Groups of related models based on both structure and semantics


