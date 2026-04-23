import pandas as pd
import numpy as np
from scipy.stats import wilcoxon


def load_ari_file(path, method_name):
    df = pd.read_csv(path, sep=None, engine="python") 

    df = df.copy()
    df["wStruct"] = pd.to_numeric(df["wStruct"], errors="coerce")
    df["wModelName"] = pd.to_numeric(df["wModelName"], errors="coerce")
    df["ARI"] = pd.to_numeric(df["ARI"], errors="coerce")

    df = df.rename(columns={"ARI": method_name})

    return df[["wStruct", "wModelName", method_name]]


def paired_cohens_d(x, y):
    diff = x - y
    diff = diff[~np.isnan(diff)]

    if len(diff) < 2:
        return np.nan

    sd = np.std(diff, ddof=1)
    if sd == 0:
        return np.nan

    return np.mean(diff) / sd


def compare_methods(df, col_a, col_b):
    sub = df[[col_a, col_b]].dropna()

    x = sub[col_a].values
    y = sub[col_b].values
    diff = x - y

    # remove zero-diff for Wilcoxon
    mask = diff != 0
    x_nz = x[mask]
    y_nz = y[mask]

    result = {
        "comparison": f"{col_a} vs {col_b}",
        "n_samples": len(sub),
        "mean_" + col_a: np.mean(x),
        "mean_" + col_b: np.mean(y),
        "mean_diff": np.mean(diff),
        "median_diff": np.median(diff),
        "cohens_d": paired_cohens_d(x, y),
        "wilcoxon_stat": np.nan,
        "p_value": np.nan
    }

    if len(x_nz) > 0:
        stat, p = wilcoxon(x_nz, y_nz)
        result["wilcoxon_stat"] = stat
        result["p_value"] = p

    return result


def main():
    max_file = "average_wStruct_wModelname_maxSimStruct_clustering_tfidf_average_specific_result.csv"
    avg_file = "average_wStruct_wModelname_avgSimStruct_clustering_tfidf_average_specific_result.csv"
    min_file = "average_wStruct_wModelname_minSimStruct_clustering_tfidf_average_specific_result.csv"

    df_max = load_ari_file(max_file, "Max")
    df_avg = load_ari_file(avg_file, "Avg")
    df_min = load_ari_file(min_file, "Min")

    merged = df_max.merge(df_avg, on=["wStruct", "wModelName"], how="inner") \
                   .merge(df_min, on=["wStruct", "wModelName"], how="inner")

    print("Paired samples:", len(merged))
    print()

    results = []
    for a, b in [("Max", "Avg"), ("Max", "Min"), ("Avg", "Min")]:
        results.append(compare_methods(merged, a, b))

    results_df = pd.DataFrame(results)

    print("=== RESULTS ===")
    print(results_df.to_string(index=False))

    results_df.to_csv("stat_results.csv", index=False)


if __name__ == "__main__":
    main()