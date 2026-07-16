CREATE TABLE jackpot_win_history (
    id BIGSERIAL PRIMARY KEY,
    jackpot_id BIGINT NOT NULL REFERENCES jackpots(id),
    won_by VARCHAR(100) NOT NULL,
    won_amount NUMERIC(19,4) NOT NULL,
    egm_id VARCHAR(50),
    won_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    spin_id VARCHAR(100) UNIQUE
);

CREATE INDEX idx_win_history_jackpot_id ON jackpot_win_history(jackpot_id);
CREATE INDEX idx_win_history_spin_id ON jackpot_win_history(spin_id);
