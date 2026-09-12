-- ==============================================================================
-- Migration: 20260912160000_init_lead_saver_schema.sql
-- Description: Production Database Schema & Security for WA Lead Saver
-- Application: WhatsApp Lead Capture & Contacts Sync
-- ==============================================================================

-- 1. Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ==============================================================================
-- 2. TABLE: public.profiles
-- Linked to Supabase Auth (auth.users)
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    full_name TEXT DEFAULT '',
    role TEXT NOT NULL DEFAULT 'user' CHECK (role IN ('admin', 'user')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==============================================================================
-- 3. TABLE: public.accounts
-- Customer subscription & licensing accounts
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_number TEXT UNIQUE NOT NULL,
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    customer_name TEXT NOT NULL,
    email TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'expired')),
    expires_at TIMESTAMPTZ,
    max_devices INT NOT NULL DEFAULT 1 CHECK (max_devices >= 1),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==============================================================================
-- 4. TABLE: public.account_devices
-- Device tracking and device limit enforcement per customer account
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.account_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID NOT NULL REFERENCES public.accounts(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    device_name TEXT DEFAULT '',
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_account_device UNIQUE (account_id, device_id)
);

-- ==============================================================================
-- 5. TABLE: public.admin_actions
-- Comprehensive audit trail for all administrative operations
-- ==============================================================================
CREATE TABLE IF NOT EXISTS public.admin_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    action_type TEXT NOT NULL,
    target_account_id UUID REFERENCES public.accounts(id) ON DELETE SET NULL,
    target_account_number TEXT,
    details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ==============================================================================
-- 6. INDEXES for Performance
-- ==============================================================================
CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_profiles_role ON public.profiles(role);

CREATE INDEX IF NOT EXISTS idx_accounts_account_number ON public.accounts(account_number);
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON public.accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON public.accounts(status);
CREATE INDEX IF NOT EXISTS idx_accounts_expires_at ON public.accounts(expires_at);

CREATE INDEX IF NOT EXISTS idx_account_devices_account_id ON public.account_devices(account_id);
CREATE INDEX IF NOT EXISTS idx_account_devices_device_id ON public.account_devices(device_id);

CREATE INDEX IF NOT EXISTS idx_admin_actions_admin_id ON public.admin_actions(admin_id);
CREATE INDEX IF NOT EXISTS idx_admin_actions_target_acc ON public.admin_actions(target_account_id);
CREATE INDEX IF NOT EXISTS idx_admin_actions_created_at ON public.admin_actions(created_at DESC);

-- ==============================================================================
-- 7. HELPER FUNCTIONS & TRIGGERS
-- ==============================================================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_profiles_updated_at ON public.profiles;
CREATE TRIGGER trg_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

DROP TRIGGER IF EXISTS trg_accounts_updated_at ON public.accounts;
CREATE TRIGGER trg_accounts_updated_at
    BEFORE UPDATE ON public.accounts
    FOR EACH ROW
    EXECUTE FUNCTION public.set_updated_at();

-- Auto-create profile when a new user signs up in auth.users
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.profiles (id, email, full_name, role)
    VALUES (
        NEW.id,
        NEW.email,
        COALESCE(NEW.raw_user_meta_data->>'full_name', ''),
        COALESCE(NEW.raw_user_meta_data->>'role', 'user')
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        updated_at = now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user();

-- Helper function: Check if current authenticated user has admin role
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
SECURITY DEFINER
STABLE
SET search_path = public
AS $$
    SELECT COALESCE(
        (SELECT role = 'admin' FROM public.profiles WHERE id = auth.uid()),
        false
    );
$$;

GRANT EXECUTE ON FUNCTION public.is_admin() TO anon, authenticated;

-- Helper function: Check if an account is currently active and not expired
CREATE OR REPLACE FUNCTION public.is_account_active(p_account_id UUID)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_status TEXT;
    v_expires_at TIMESTAMPTZ;
BEGIN
    SELECT status, expires_at INTO v_status, v_expires_at
    FROM public.accounts
    WHERE id = p_account_id;

    IF NOT FOUND THEN
        RETURN FALSE;
    END IF;

    IF v_status <> 'active' THEN
        RETURN FALSE;
    END IF;

    IF v_expires_at IS NOT NULL AND v_expires_at < now() THEN
        RETURN FALSE;
    END IF;

    RETURN TRUE;
END;
$$;

GRANT EXECUTE ON FUNCTION public.is_account_active(UUID) TO authenticated;

-- ==============================================================================
-- 8. CORE RPC FUNCTIONS USED BY ANDROID APP
-- ==============================================================================

-- Lookup customer email by account number for seamless login flow
CREATE OR REPLACE FUNCTION public.get_account_email_by_number(p_account_number TEXT)
RETURNS TEXT
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_email TEXT;
BEGIN
    SELECT email INTO v_email
    FROM public.accounts
    WHERE UPPER(TRIM(account_number)) = UPPER(TRIM(p_account_number))
    LIMIT 1;

    RETURN v_email;
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_account_email_by_number(TEXT) TO anon, authenticated;

-- Validate account status, expiration, and enforce device limits
CREATE OR REPLACE FUNCTION public.register_account_device(
    p_account_id UUID,
    p_device_id TEXT,
    p_device_name TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_status TEXT;
    v_expires_at TIMESTAMPTZ;
    v_max_dev INT;
    v_curr_count INT;
    v_already_reg BOOLEAN;
BEGIN
    -- 1. Check account existence and status
    SELECT status, expires_at, max_devices
    INTO v_status, v_expires_at, v_max_dev
    FROM public.accounts
    WHERE id = p_account_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'message', 'الحساب غير مسجل أو غير موجود');
    END IF;

    IF v_status = 'inactive' THEN
        RETURN jsonb_build_object('success', false, 'message', 'تم إيقاف هذا الحساب من قبل الإدارة');
    END IF;

    IF v_status = 'expired' OR (v_expires_at IS NOT NULL AND v_expires_at < now()) THEN
        RETURN jsonb_build_object('success', false, 'message', 'انتهت صلاحية هذا الحساب');
    END IF;

    -- 2. If device is already registered, refresh last active time
    SELECT EXISTS (
        SELECT 1 FROM public.account_devices
        WHERE account_id = p_account_id AND device_id = p_device_id
    ) INTO v_already_reg;

    IF v_already_reg THEN
        UPDATE public.account_devices
        SET last_active_at = now(),
            device_name = COALESCE(NULLIF(TRIM(p_device_name), ''), device_name)
        WHERE account_id = p_account_id AND device_id = p_device_id;

        RETURN jsonb_build_object('success', true, 'message', 'Device verified');
    END IF;

    -- 3. Check device limits
    SELECT count(*) INTO v_curr_count
    FROM public.account_devices
    WHERE account_id = p_account_id;

    IF v_curr_count >= COALESCE(v_max_dev, 1) THEN
        RETURN jsonb_build_object(
            'success', false,
            'message', 'تم الوصول إلى الحد الأقصى للأجهزة المسموح بها (' || COALESCE(v_max_dev, 1) || ')'
        );
    END IF;

    -- 4. Register new device
    INSERT INTO public.account_devices(account_id, device_id, device_name, registered_at, last_active_at)
    VALUES (p_account_id, p_device_id, COALESCE(TRIM(p_device_name), ''), now(), now());

    RETURN jsonb_build_object('success', true, 'message', 'Device registered successfully');
END;
$$;

GRANT EXECUTE ON FUNCTION public.register_account_device(UUID, TEXT, TEXT) TO authenticated;

-- Audit logger for administrative actions
CREATE OR REPLACE FUNCTION public.log_admin_action(
    p_action_type TEXT,
    p_target_account_id UUID DEFAULT NULL,
    p_target_account_number TEXT DEFAULT NULL,
    p_details JSONB DEFAULT '{}'::jsonb
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_log_id UUID;
BEGIN
    IF NOT public.is_admin() THEN
        RAISE EXCEPTION 'Unauthorized: Only administrators can record admin actions';
    END IF;

    INSERT INTO public.admin_actions (
        admin_id,
        action_type,
        target_account_id,
        target_account_number,
        details
    ) VALUES (
        auth.uid(),
        p_action_type,
        p_target_account_id,
        p_target_account_number,
        p_details
    ) RETURNING id INTO v_log_id;

    RETURN v_log_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.log_admin_action(TEXT, UUID, TEXT, JSONB) TO authenticated;

-- Helper to grant admin role to a user by email
CREATE OR REPLACE FUNCTION public.make_user_admin(p_email TEXT)
RETURNS BOOLEAN
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    UPDATE public.profiles
    SET role = 'admin', updated_at = now()
    WHERE lower(email) = lower(trim(p_email));
    
    RETURN FOUND;
END;
$$;

-- ==============================================================================
-- 9. ROW-LEVEL SECURITY (RLS) POLICIES
-- ==============================================================================

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.accounts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.account_devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.admin_actions ENABLE ROW LEVEL SECURITY;

-- ------------------------------------------------------------------------------
-- PROFILES POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "profiles_select_policy" ON public.profiles;
CREATE POLICY "profiles_select_policy"
ON public.profiles
FOR SELECT
TO authenticated
USING (id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "profiles_insert_policy" ON public.profiles;
CREATE POLICY "profiles_insert_policy"
ON public.profiles
FOR INSERT
TO authenticated
WITH CHECK (id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "profiles_update_policy" ON public.profiles;
CREATE POLICY "profiles_update_policy"
ON public.profiles
FOR UPDATE
TO authenticated
USING (id = auth.uid() OR public.is_admin())
WITH CHECK (id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "profiles_delete_policy" ON public.profiles;
CREATE POLICY "profiles_delete_policy"
ON public.profiles
FOR DELETE
TO authenticated
USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- ACCOUNTS POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "accounts_select_policy" ON public.accounts;
CREATE POLICY "accounts_select_policy"
ON public.accounts
FOR SELECT
TO authenticated
USING (user_id = auth.uid() OR public.is_admin());

DROP POLICY IF EXISTS "accounts_insert_policy" ON public.accounts;
CREATE POLICY "accounts_insert_policy"
ON public.accounts
FOR INSERT
TO authenticated
WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "accounts_update_policy" ON public.accounts;
CREATE POLICY "accounts_update_policy"
ON public.accounts
FOR UPDATE
TO authenticated
USING (public.is_admin())
WITH CHECK (public.is_admin());

DROP POLICY IF EXISTS "accounts_delete_policy" ON public.accounts;
CREATE POLICY "accounts_delete_policy"
ON public.accounts
FOR DELETE
TO authenticated
USING (public.is_admin());

-- ------------------------------------------------------------------------------
-- ACCOUNT_DEVICES POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "devices_select_policy" ON public.account_devices;
CREATE POLICY "devices_select_policy"
ON public.account_devices
FOR SELECT
TO authenticated
USING (
    account_id IN (SELECT id FROM public.accounts WHERE user_id = auth.uid())
    OR public.is_admin()
);

DROP POLICY IF EXISTS "devices_insert_policy" ON public.account_devices;
CREATE POLICY "devices_insert_policy"
ON public.account_devices
FOR INSERT
TO authenticated
WITH CHECK (
    account_id IN (SELECT id FROM public.accounts WHERE user_id = auth.uid())
    OR public.is_admin()
);

DROP POLICY IF EXISTS "devices_update_policy" ON public.account_devices;
CREATE POLICY "devices_update_policy"
ON public.account_devices
FOR UPDATE
TO authenticated
USING (
    account_id IN (SELECT id FROM public.accounts WHERE user_id = auth.uid())
    OR public.is_admin()
)
WITH CHECK (
    account_id IN (SELECT id FROM public.accounts WHERE user_id = auth.uid())
    OR public.is_admin()
);

DROP POLICY IF EXISTS "devices_delete_policy" ON public.account_devices;
CREATE POLICY "devices_delete_policy"
ON public.account_devices
FOR DELETE
TO authenticated
USING (
    account_id IN (SELECT id FROM public.accounts WHERE user_id = auth.uid())
    OR public.is_admin()
);

-- ------------------------------------------------------------------------------
-- ADMIN_ACTIONS POLICIES
-- ------------------------------------------------------------------------------
DROP POLICY IF EXISTS "admin_actions_select_policy" ON public.admin_actions;
CREATE POLICY "admin_actions_select_policy"
ON public.admin_actions
FOR SELECT
TO authenticated
USING (public.is_admin());

DROP POLICY IF EXISTS "admin_actions_insert_policy" ON public.admin_actions;
CREATE POLICY "admin_actions_insert_policy"
ON public.admin_actions
FOR INSERT
TO authenticated
WITH CHECK (public.is_admin());

-- ==============================================================================
-- 10. GRANTS
-- ==============================================================================
GRANT USAGE ON SCHEMA public TO anon, authenticated;
GRANT ALL ON ALL TABLES IN SCHEMA public TO authenticated;
GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO authenticated;
