#!/usr/bin/env python3
"""Small concurrent HTTP load probe. Reports measurements observed by this run only."""
import argparse, concurrent.futures, statistics, time, urllib.request

def main():
    p=argparse.ArgumentParser(description=__doc__); p.add_argument('url'); p.add_argument('--requests',type=int,default=100); p.add_argument('--concurrency',type=int,default=10); a=p.parse_args()
    def hit(_):
        start=time.perf_counter()
        try:
            with urllib.request.urlopen(a.url,timeout=10) as r: r.read(); return (time.perf_counter()-start)*1000, r.status < 400
        except Exception: return (time.perf_counter()-start)*1000, False
    start=time.perf_counter()
    with concurrent.futures.ThreadPoolExecutor(max_workers=a.concurrency) as pool: results=list(pool.map(hit,range(a.requests)))
    elapsed=time.perf_counter()-start; lat=[x for x,_ in results]; ok=sum(1 for _,success in results if success)
    print(f'requests={len(results)} concurrency={a.concurrency} elapsed_s={elapsed:.3f}')
    print(f'throughput_rps={len(results)/elapsed:.2f} success={ok} errors={len(results)-ok} error_rate_pct={(len(results)-ok)*100/len(results):.2f}')
    print(f'latency_ms_avg={statistics.mean(lat):.2f} p95={sorted(lat)[min(len(lat)-1,int(len(lat)*.95))]:.2f} p99={sorted(lat)[min(len(lat)-1,int(len(lat)*.99))]:.2f}')
if __name__=='__main__': main()
