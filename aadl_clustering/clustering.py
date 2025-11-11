import os
import re
import json
import pandas as pd
import numpy as np
from itertools import combinations
from scipy.cluster.hierarchy import linkage, fcluster
from scipy.spatial.distance import squareform
from sklearn.metrics import adjusted_rand_score
import matplotlib.pyplot as plt
from collections import defaultdict

def load_similarity_matrix(csv_file_path):
    df = pd.read_csv(csv_file_path, index_col=0)
    return df.values, df.columns.tolist()

def compute_ari(clusters, ground_truth, model_names):
    cluster_df = pd.DataFrame({'Model': model_names, 'Cluster': clusters})
    cluster_df['Model'] = cluster_df['Model'].str.replace('.json', '', regex=True)
    ground_truth['Model'] = ground_truth['Model'].str.replace('.aaxl2', '', regex=True)
    merged_df = pd.merge(cluster_df, ground_truth, on='Model')
    if len(merged_df) < 2:
        return -1, 0  # too few models to compute ARI
    ari = adjusted_rand_score(merged_df['CLUSTERS'].values, merged_df['Cluster'].values)
    return ari, len(set(clusters)), merged_df

def get_wstruct_from_filename(filename):
    match = re.search(r'_wStruct_([\d.]+)_wModelname_([\d.]+)\.csv', filename)
    if match:
        w_struct = float(match.group(1))
        w_modelname = float(match.group(2))
    else:
        print("No match")
    return w_struct, w_modelname

def merge_clusters(clusters, similarity_matrix, model_names, ground_truth,
                   merge_threshold=0.8, option="merge_after_best_ARI", linkage_method="average"):
    """
    Merge clusters based on average similarity between clusters.

    Parameters:
    - clusters: np.array, initial cluster labels for each model.
    - similarity_matrix: np.array, NxN similarity values between models.
    - model_names: list of model names (order matches similarity_matrix and clusters).
    - ground_truth: pd.DataFrame, optional, used only when option == "merge_after_best_ARI".
    - merge_threshold: float, default 0.8, min similarity to merge two clusters.
    - option: "merge_after_cutoff", "merge_after_best_ARI"
    - linkage_method: "average" or "complete" for cluster similarity calc.

    Returns:
    - new_labels: np.array, final cluster labels after merge.
    - merge_log: list of tuples (cluster_a, cluster_b, similarity, ARI_after_merge, num_clusters)
    """
    labels = np.array(clusters)
    unique_clusters = sorted(set(labels))

    # Build mapping: cluster_id -> indices of models
    cluster_members = {cid: np.where(labels == cid)[0].tolist() for cid in unique_clusters}

    # Cache for average similarities between clusters
    def compute_cluster_similarity(c1, c2):
        idx1, idx2 = cluster_members[c1], cluster_members[c2]
        sims = similarity_matrix[np.ix_(idx1, idx2)]
        if linkage_method == "average":
            return sims.mean()
        elif linkage_method == "complete":
            return sims.min()
        else:
            raise ValueError("Unsupported linkage_method for merging.")

    merge_log = []

    best_ari , no_clusters , merged_df = compute_ari(clusters, ground_truth, model_names)

    while True:
        # Find most similar pair of clusters
        best_pair = None
        best_sim = -1
        for c1, c2 in combinations(cluster_members.keys(), 2):
            sim = compute_cluster_similarity(c1, c2)
            if sim > best_sim:
                best_sim = sim
                best_pair = (c1, c2)

        # Merge clusters
        c1, c2 = best_pair
        new_id = min(c1, c2)  # keep smaller ID
        cluster_members[new_id] = cluster_members[c1] + cluster_members[c2]

        if c1 > c2:
            del cluster_members[c1]
        else:
            del cluster_members[c2]

        # Update labels
        labels[labels == c1] = new_id
        labels[labels == c2] = new_id

        # Reassign cluster IDs to be continuous
        unique_clusters = sorted(cluster_members.keys())
        mapping = {old: i+1 for i, old in enumerate(unique_clusters)}
        labels = np.array([mapping[l] for l in labels])
        cluster_members = {mapping[old]: [i for i, l in enumerate(labels) if l == mapping[old]]
                           for old in unique_clusters}
        
        # Compute ARI if needed
        ari_after, no_clusters_after, merged_df_after = compute_ari(labels, ground_truth, model_names)
        import pdb;pdb.set_trace()
        # Option stop condition
        if option == "merge_after_best_ARI" and ari_after is not None:
            if ari_after < best_ari:
                # revert last merge
                return clusters, best_ari , no_clusters
            else:
                best_ari = ari_after

        merge_log.append((c1, c2, best_sim, ari_after, len(set(labels))))
    return labels, best_ari, len(set(labels))


def process_all_matrices(input_matrix_folder, validate_csv, linkage_method):
    output_results = []
    best_merged_df = None
    for file in os.listdir(input_matrix_folder):
        if file.endswith(".csv") and "_wStruct_" in file:
            full_path = os.path.join(input_matrix_folder, file)
            print(f"Processing: {file}")

            # Load matrix
            similarity_matrix, model_names = load_similarity_matrix(full_path)
            similarity_matrix = np.clip(similarity_matrix, 0.0, 1.0)
            distance_matrix = 1 - similarity_matrix
            distance_matrix = (distance_matrix + distance_matrix.T) / 2
            distance_vector = squareform(distance_matrix, checks=False)
            linkage_matrix = linkage(distance_vector, method = linkage_method) #ward, average, complete, single

            # Ground truth
            ground_truth = pd.read_csv(validate_csv)

            best_ari = -1
            best_num_clusters = 0
            best_clusters = None
            best_merged_df = None
            for cutoff in np.arange(0.0, 1.05, 0.05):
                clusters = fcluster(linkage_matrix, cutoff, criterion='distance')
                ari, num_clusters, merged_df = compute_ari(clusters, ground_truth, model_names)
                if ari > best_ari:
                    best_ari = ari
                    best_num_clusters = num_clusters
                    best_clusters = clusters
                    best_merged_df = merged_df
            
            # last_modified_cluster, best_ari, best_num_clusters = merge_clusters(best_clusters, similarity_matrix, model_names, ground_truth=ground_truth, merge_threshold=0.8, option="merge_after_best_ARI", linkage_method=linkage_method)
            wStruct, w_modelname = get_wstruct_from_filename(file)

            output_results.append({
                "wStruct": wStruct,
                "wModelName": w_modelname,
                "ARI": round(best_ari, 4),
                "NumberOfClusters": best_num_clusters
            })
    return output_results, best_merged_df

def main():
    with open('config.json') as f:
        config = json.load(f)

    input_matrix_folder = config['input_matrix_folder']
    validate_csv = config['validate_csv']
    domain = config['domain']
    linkage_method = config['linkage_method']
    embedding_method = config['embedding_method']

    results, best_merged_df = process_all_matrices(input_matrix_folder, validate_csv, linkage_method)

    folder_name = os.path.basename(os.path.normpath(input_matrix_folder))
    result_csv_name = f"{folder_name}_clustering_{embedding_method}_{linkage_method}_{domain}_result.csv"
    df_result = pd.DataFrame(results)
    df_result.to_csv(result_csv_name, index=False)
    print(f"Clustering results saved to: {result_csv_name}")

if __name__ == "__main__":
    main()
