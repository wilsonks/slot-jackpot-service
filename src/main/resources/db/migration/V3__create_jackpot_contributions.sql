CREATE TABLE jackpot_contributions (
    id BIGSERIAL PRIMARY KEY,
    jackpot_id BIGINT NOT NULL REFERENCES jackpots(id),
    spin_id VARCHAR(100) NOT NULL UNIQUE,
    bet_amount NUMERIC(19,4) NOT NULL,
    contribution_amount NUMERIC(19,4) NOT NULL,
    egm_id VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contributions_jackpot_id ON jackpot_contributions(jackpot_id);
CREATE INDEX idx_contributions_spin_id ON jackpot_contributions(spin_id);
