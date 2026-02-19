from __future__ import annotations

import os
import sys
import csv
import time
import datetime as dt
from dataclasses import dataclass
from typing import Dict, Any, List, Tuple, Optional

import requests

GITHUB_API = "https://api.github.com"
SEARCH_REPOS_ENDPOINT = f"{GITHUB_API}/search/repositories"
RATE_LIMIT_ENDPOINT = f"{GITHUB_API}/rate_limit"


@dataclass
class QuerySpec:
    name: str
    q_core: str


DEFAULT_QUERIES: List[QuerySpec] = [
    QuerySpec(
        name="text:aadl in name/desc/readme",
        q_core="aadl in:name,description,readme archived:false is:public"
    ),
    QuerySpec(
        name="topic:aadl",
        q_core="topic:aadl archived:false is:public"
    ),
]


def iso_date(d: dt.date) -> str:
    return d.strftime("%Y-%m-%d")


def month_ranges(start_date: dt.date, end_date: dt.date) -> List[Tuple[dt.date, dt.date]]:
    """Return list of (month_start, month_end) inclusive."""
    ranges: List[Tuple[dt.date, dt.date]] = []
    cur = dt.date(start_date.year, start_date.month, 1)
    while cur <= end_date:
        if cur.month == 12:
            nxt = dt.date(cur.year + 1, 1, 1)
        else:
            nxt = dt.date(cur.year, cur.month + 1, 1)
        month_end = min(end_date, nxt - dt.timedelta(days=1))
        ranges.append((cur, month_end))
        cur = nxt
    return ranges


def gh_headers(token: str) -> Dict[str, str]:
    return {
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "aadl-top-updated-repos-script",
    }


def request_with_backoff(
    sess: requests.Session,
    url: str,
    headers: Dict[str, str],
    params: Optional[Dict[str, Any]] = None,
    max_retries: int = 8,
) -> requests.Response:
    delay = 1.5
    for attempt in range(max_retries):
        r = sess.get(url, headers=headers, params=params, timeout=45)

        # Hard rate limit
        if r.status_code == 403 and "rate limit" in r.text.lower():
            reset = r.headers.get("X-RateLimit-Reset")
            if reset:
                reset_ts = int(reset)
                now_ts = int(time.time())
                sleep_s = max(1, reset_ts - now_ts + 2)
                print(f"[rate-limit] sleeping {sleep_s}s until reset...", file=sys.stderr)
                time.sleep(sleep_s)
                continue

        # Secondary rate limit / abuse detection
        if r.status_code in (403, 429) and ("secondary rate" in r.text.lower() or "abuse" in r.text.lower()):
            print(f"[secondary-limit] sleeping {int(delay)}s then retry...", file=sys.stderr)
            time.sleep(delay)
            delay *= 2
            continue

        if 200 <= r.status_code < 300:
            return r

        if r.status_code in (500, 502, 503, 504):
            print(f"[server {r.status_code}] sleeping {int(delay)}s then retry...", file=sys.stderr)
            time.sleep(delay)
            delay *= 2
            continue

        r.raise_for_status()

    raise RuntimeError(f"Failed after {max_retries} retries: {url} params={params}")


def get_rate_limit(sess: requests.Session, headers: Dict[str, str]) -> Dict[str, Any]:
    r = request_with_backoff(sess, RATE_LIMIT_ENDPOINT, headers, params=None)
    return r.json()


def build_query(q_core: str, m_start: dt.date, m_end: dt.date) -> str:
    pushed = f"pushed:{iso_date(m_start)}..{iso_date(m_end)}"
    return f"{q_core} {pushed}"


def search_top_repos(
    sess: requests.Session,
    headers: Dict[str, str],
    q: str,
    top_n: int = 10,
) -> Dict[str, Any]:
    """
    Fetch top repos for query q sorted by updated desc.
    Returns JSON response including items.
    """
    per_page = min(100, max(1, top_n))
    params = {
        "q": q,
        "sort": "updated",
        "order": "desc",
        "per_page": per_page,
        "page": 1,
    }
    r = request_with_backoff(sess, SEARCH_REPOS_ENDPOINT, headers, params=params)
    return r.json()


def main():
    token = os.getenv("GITHUB_TOKEN")
    if not token:
        print("ERROR: Set GITHUB_TOKEN env var.", file=sys.stderr)
        sys.exit(1)

    start = dt.date(2025, 1, 1)
    today = dt.date.today()
    ranges = month_ranges(start, today)

    # settings
    TOP_N = int(os.getenv("TOP_N", "10")) 
    SLEEP_BETWEEN_REQ = float(os.getenv("SLEEP", "1.2"))

    out_top_csv = "aadl_top_updated_repos_monthly.csv"
    out_counts_csv = "aadl_repo_counts_monthly_from_top.csv"

    rows_top: List[Dict[str, Any]] = []

    with requests.Session() as sess:
        headers = gh_headers(token)

        try:
            rl = get_rate_limit(sess, headers)
            search_rem = rl["resources"]["search"]["remaining"]
            core_rem = rl["resources"]["core"]["remaining"]
            print(f"[quota] search_remaining={search_rem} core_remaining={core_rem}", file=sys.stderr)
        except Exception:
            pass

        for spec in DEFAULT_QUERIES:
            print(f"\n=== Query: {spec.name} ===", file=sys.stderr)

            for (m_start, m_end) in ranges:
                q = build_query(spec.q_core, m_start, m_end)

                data = search_top_repos(sess, headers, q, top_n=TOP_N)
                total_count = int(data.get("total_count", 0))
                incomplete = bool(data.get("incomplete_results", False))
                items = data.get("items", [])[:TOP_N]

                print(
                    f"{iso_date(m_start)}..{iso_date(m_end)}  total={total_count}  "
                    f"top_returned={len(items)}  incomplete={incomplete}",
                    file=sys.stderr,
                )

                for rank, repo in enumerate(items, start=1):
                    owner = repo.get("owner", {}) or {}
                    rows_top.append({
                        "query_name": spec.name,
                        "month_start": iso_date(m_start),
                        "month_end": iso_date(m_end),
                        "rank": rank,
                        "full_name": repo.get("full_name"),
                        "html_url": repo.get("html_url"),
                        "pushed_at": repo.get("pushed_at"),
                        "updated_at": repo.get("updated_at"),
                        "created_at": repo.get("created_at"),
                        "stargazers_count": repo.get("stargazers_count"),
                        "forks_count": repo.get("forks_count"),
                        "open_issues_count": repo.get("open_issues_count"),
                        "language": repo.get("language"),
                        "owner_login": owner.get("login"),
                        "description": (repo.get("description") or "").replace("\n", " ").strip(),
                        "api_total_count_for_month": total_count,
                        "incomplete_results": incomplete,
                        "q": q,
                    })

                time.sleep(SLEEP_BETWEEN_REQ)

    top_fields = [
        "query_name", "month_start", "month_end", "rank",
        "full_name", "html_url",
        "pushed_at", "updated_at", "created_at",
        "stargazers_count", "forks_count", "open_issues_count", "language",
        "owner_login", "description",
        "api_total_count_for_month", "incomplete_results", "q",
    ]
    with open(out_top_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=top_fields)
        w.writeheader()
        w.writerows(rows_top)

    counts_map: Dict[Tuple[str, str, str], set] = {}
    incomplete_map: Dict[Tuple[str, str, str], bool] = {}

    for r in rows_top:
        key = (r["query_name"], r["month_start"], r["month_end"])
        counts_map.setdefault(key, set()).add(r["full_name"])
        incomplete_map[key] = bool(r["incomplete_results"])

    count_rows = []
    for (query_name, ms, me), s in sorted(counts_map.items()):
        count_rows.append({
            "query_name": query_name,
            "month_start": ms,
            "month_end": me,
            "unique_repos_in_topN": len(s),
            "incomplete_results": incomplete_map.get((query_name, ms, me), False),
        })

    with open(out_counts_csv, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=["query_name", "month_start", "month_end", "unique_repos_in_topN", "incomplete_results"])
        w.writeheader()
        w.writerows(count_rows)

    print(f"\nWrote:\n- {out_top_csv}\n- {out_counts_csv}")


if __name__ == "__main__":
    main()